同泰高导质检Android客户端
====

# App

## 发布构建

```shell
./gradlew asR
```

APK文件生成在`app/build/outputs/apk/release/app-release.apk`，可直接安装。

# DAO服务器

因为在Android直接访问MySQL有技术难度，所以通过一个DAO中间层服务器，提供RESTful API。

## 构建

1. 请确保有[Rust](https://www.rust-lang.org/)环境
2. 编译
   ```shell
   cd dao
   cargo build -r
   ```
   
   支持交叉编译，例如目标平台为Windows：
   ```shell
   rustup target add x86_64-pc-windows-gnu
   cd dao
   cargo build -r --target x86_64-pc-windows-gnu
   ```
   
   构建产物在`dao/target/x86_64-pc-windows-gnu/release/czttgd-dao`（交叉编译）或`dao/target/release/czttgd-dao`（本地目标编译）
   
   注：如果目标为Windows平台，编译产物后面加上`.exe`扩展名，如`czttgd-dao.exe`。下方描述中均省略`.exe`。
3. 参考`dao/server.toml.template`创建配置文件，配置文件名为`server.toml`，放在与`czttgd-dao`同一目录下。
4. 执行`czttgd-dao`。

启动了DAO服务器后，就可在App里主页右上角的设置中配置此服务器地址，`http://ip:port`的形式。
