# Gradle ビルド問題の知見

Date: 2026-01-25
Context: INFRA-03（共通依存関係設定）実装時に発生したGradleビルドの問題

---

## 1. OpenTelemetry Instrumentation BOM バージョン問題

### 症状

```
Could not find io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:1.45.0
Could not find io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:.
```

### 原因

OpenTelemetry のコアライブラリと Instrumentation ライブラリは**異なるバージョン体系**を使用している：

| ライブラリ | バージョン体系 | 例 |
|-----------|--------------|-----|
| `io.opentelemetry:opentelemetry-bom` | 1.x | 1.45.0, 1.55.0 |
| `io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom` | 2.x | 2.12.0, 2.24.0 |

同じバージョン番号（1.45.0）を両方に適用しようとしたため、Instrumentation BOM が見つからなかった。

### 解決方法

バージョンカタログで別々のバージョンを管理：

```groovy
ext {
    versions = [
        opentelemetry      : '1.45.0',      // コアライブラリ用
        opentelemetryInstr : '2.24.0',      // Instrumentation用（別バージョン体系）
    ]
}
```

BOM imports で正しいバージョンを参照：

```groovy
mavenBom "io.opentelemetry:opentelemetry-bom:${rootProject.versions.opentelemetry}"
mavenBom "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${rootProject.versions.opentelemetryInstr}"
```

---

## 2. OpenTelemetry と Spring Boot 4.x の互換性問題

### 症状

```
ClassNotFoundException: org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration

Failed to generate bean name for imported class 
'io.opentelemetry.instrumentation.spring.autoconfigure.internal.instrumentation.web.RestClientInstrumentationAutoConfiguration'
```

### 原因

OpenTelemetry Instrumentation の古いバージョン（2.12.0）が Spring Boot 3.x 向けに作られており、
Spring Boot 4.x で移動/リネームされたクラス（`RestClientAutoConfiguration`）を参照していた。

### 解決方法

OpenTelemetry Instrumentation を最新バージョンに更新：

```groovy
opentelemetryInstr : '2.24.0'  // Spring Boot 4.x 互換
```

### 確認方法

Maven Central で対応バージョンを確認：
```bash
curl -s "https://repo.maven.apache.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-bom/maven-metadata.xml" | grep '<version>'
```

---

## 3. DataSource AutoConfiguration 問題

### 症状

```
DataSourceProperties$DataSourceBeanCreationException
Failed to configure a DataSource: 'url' attribute is not specified
```

### 原因

`spring-boot-starter-data-jpa` が依存関係にあると、Spring Boot が自動的に DataSource を構成しようとする。
テスト時に DB 設定がないと失敗する。

### 解決方法

#### 方法A: Testcontainers を使用（推奨）

`bootstrap/build.gradle`:
```groovy
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:postgresql'
```

`src/test/resources/application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16-alpine:///booking_payment_test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration
```

#### 方法B: AutoConfiguration を除外（一時的な回避策）

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
```

**注意**: 方法B は本来のテストを実行できないため、方法A を推奨。

---

## 4. Gradle Binary Store エラー（環境依存）

### 症状

```
Problems reading data from Binary store in /tmp/cursor-sandbox-cache/.../gradle/.tmp/gradle*.bin offset * exists? true
```

### 原因

Cursor IDE のサンドボックス環境で、Gradle の一時ファイル（Binary Store）へのアクセスに問題が発生。
サンドボックスの制限や、異なるセッション間でのキャッシュ不整合が原因。

### 解決方法

1. Gradle キャッシュをクリア：
```bash
rm -rf ~/.gradle/.tmp
rm -rf ~/.gradle/caches
```

2. サンドボックス外で実行（`required_permissions: ["all"]`）

3. ビルドオプションを追加：
```bash
./gradlew clean build --no-daemon --no-build-cache
```

### 注意

このエラーはコードの問題ではなく環境の問題。コードを変更せずにビルドオプションや環境設定で対処する。

---

## 5. dependency-management プラグインの適用

### 症状

BOM をインポートしても依存関係のバージョンが解決されない（バージョンが空になる）。

### 原因

`bootstrap` モジュールがルートの `subprojects` 設定から除外されており、
`io.spring.dependency-management` プラグインが適用されていなかった。

### 解決方法

#### 方法A: platform() API を使用（推奨）

Gradle ネイティブの `platform()` を使用（dependency-management プラグイン不要）：

```groovy
dependencies {
    implementation platform("org.springframework.boot:spring-boot-dependencies:${rootProject.versions.springBoot}")
    implementation platform("io.opentelemetry:opentelemetry-bom:${rootProject.versions.opentelemetry}")
    
    // バージョン指定不要
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

#### 方法B: dependency-management プラグインを使用

```groovy
plugins {
    id 'io.spring.dependency-management'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:${springBootVersion}"
    }
}
```

**注意**: `dependencyManagement` ブロック内での変数参照は、ローカル変数として事前に定義するか、`rootProject.ext.versions` を使用する。

---

## トラブルシューティング手順

1. **依存関係の解決を確認**:
```bash
./gradlew :bootstrap:dependencies --configuration compileClasspath
```

2. **詳細なエラーを確認**:
```bash
./gradlew build --stacktrace
```

3. **キャッシュをクリアして再ビルド**:
```bash
./gradlew clean build --no-daemon --refresh-dependencies
```

4. **Maven Central でバージョン存在確認**:
```bash
curl -s "https://repo.maven.apache.org/maven2/<group>/<artifact>/maven-metadata.xml" | grep '<version>'
```
