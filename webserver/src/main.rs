use actix_web::{web, App, HttpServer};
use img_dumpster::routes::exif_test::read_exifs;
use img_dumpster::routes::hello_world::hello_world;
use img_dumpster::routes::uploads::post::upload_multi_part_file;
use img_dumpster::routes::images::post::upload_images;
use std::net::TcpListener;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let _server = HttpServer::new(move || {
        App::new()
            .route("/hello_world", web::get().to(hello_world))
            .route("/exif_test", web::get().to(read_exifs))
            .route("/file", web::post().to(upload_multi_part_file))
            .route("/images", web::post().to(upload_images))
    })
    .listen(TcpListener::bind("127.0.0.1:8002")?)?
    .run()
    .await;

    Ok(())
}
