use actix_multipart::form::tempfile::TempFile;
use actix_multipart::form::MultipartForm;
use actix_web::HttpResponse;
use uuid::Uuid;
use serde::Serialize;

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
}

#[tracing::instrument(skip(form))]
pub async fn upload_multi_part_file(
    MultipartForm(form): MultipartForm<UploadForm>,
) -> HttpResponse {
    dbg!(&form);
    let mut ids = Vec::new();

    for f in form.files {
        let id = Uuid::new_v5(&Default::default(), &[]).to_string();
        let path = format!(
            "/tmp/{}-{}",
            &id,
            &f.file_name.unwrap()
        );
        dbg!(format!("saving to {path}"));
        f.file
            .persist(&path)
            .expect("Something happened with the save");

        ids.push(UploadedFile { id, path });
    }

    HttpResponse::Ok().json(ids)
}
