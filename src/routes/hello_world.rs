use actix_web::HttpResponse;

pub async fn hello_world() -> HttpResponse {
    HttpResponse::Ok().body("Hello world!".to_string())
}
