use actix_web::HttpResponse;
use std::collections::HashMap;
use std::{env, fs};

fn read_file(path: String) -> HashMap<String, String> {
    dbg!(&path);
    let mut exif_map = HashMap::new();
    let file = fs::File::open(path).expect("file has to open");
    let mut bufreader = std::io::BufReader::new(&file);
    let exifreader = exif::Reader::new();
    let exif = exifreader
        .read_from_container(&mut bufreader)
        .expect("should be able to read");

    for f in exif.fields() {
        let key = format!("{}.{}", f.tag, f.ifd_num);
        exif_map.insert(key, f.display_value().with_unit(&exif).to_string());
    }
    exif_map
}

pub async fn read_exifs() -> HttpResponse {
    let base_dir = env::current_dir()
        .expect("could not create base path")
        .display()
        .to_string();
    let exif_data = read_file(format!("{base_dir}/sample_photos/20181011_123225.jpg"));
    read_file(format!("{base_dir}/sample_photos/20221114_211927.jpg"));

    HttpResponse::Ok().json(exif_data)
}
