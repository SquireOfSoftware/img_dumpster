use actix_multipart::form::tempfile::TempFile;
use actix_multipart::form::MultipartForm;
use actix_web::HttpResponse;
use uuid::Uuid;

#[derive(Debug, MultipartForm)]
pub struct UploadForm {
    #[multipart(rename = "file")]
    files: Vec<TempFile>,
}

#[tracing::instrument(skip(form))]
pub async fn upload_multi_part_file(
    MultipartForm(form): MultipartForm<UploadForm>,
) -> HttpResponse {
    dbg!(&form);

    for f in form.files {
        let path = format!(
            "./{}-{}",
            &Uuid::new_v5(&Default::default(), &[]).to_string(),
            &f.file_name.unwrap()
        );
        dbg!(format!("saving to {path}"));
        let new_file = f
            .file
            .persist(path)
            .expect("Something happened with the save");

        dbg!(new_file.metadata()).expect("where is metadata?");
    }
    HttpResponse::Ok().body("Upload is done!".to_string())
}
