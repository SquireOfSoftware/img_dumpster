use actix_web::{web, App, HttpServer};
use img_dumpster::routes::hello_world::hello_world;
use std::net::TcpListener;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let _server = HttpServer::new(move || App::new().route("/test", web::get().to(hello_world)))
        .listen(TcpListener::bind("127.0.0.1:8002")?)?
        .run()
        .await;

    Ok(())
}
