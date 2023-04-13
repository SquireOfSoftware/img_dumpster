use actix_multipart::form::tempfile::TempFile;
use actix_multipart::form::MultipartForm;
use actix_web::HttpResponse;
use uuid::Uuid;
use serde::Serialize;
use sha2::{Sha256, Digest};
use std::io::Read;

#[derive(Debug, MultipartForm)]
pub struct UploadForm {
    #[multipart(rename = "file")]
    files: Vec<TempFile>,
}

#[derive(Debug, Serialize)]
struct UploadedResponse {
    uploads: Vec<UploadedFile>
}

#[derive(Debug, Serialize)]
struct UploadedFile {
    id: String,
    path: String,
    file_hash: String,
}

pub async fn upload_images(MultipartForm(form): MultipartForm<UploadForm>,
) -> HttpResponse {
    dbg!(&form);
    let mut ids = Vec::new();

    for mut f in form.files {
        let id = Uuid::new_v4().to_string();
        let path = format!(
            "/tmp/{}-{}",
            &id,
            &f.file_name.unwrap()
        );
        let mut file_bytes = Vec::new();
        let _ = &f.file.read_to_end(&mut file_bytes).expect("Unable to read data");
        let mut file_hash = Sha256::new();
        file_hash.update(&file_bytes);
        // Sha256::digest(&file_bytes);
        let file_hash = format!("{:x}", &file_hash.finalize());
        dbg!(format!("saving to {path}, hash: {file_hash}"));
        f.file
            .persist(&path)
            .expect("Something happened with the save");

        ids.push(UploadedFile { id, path, file_hash });
    }

    HttpResponse::Ok().json(ids)
}