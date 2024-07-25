#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod commands;
mod log;
mod media;
mod state;
mod tasks;

use crate::{commands::AppEvent, log::{AppLogLayer, LogEvent}, state::AppState};

use color_eyre::Result;
use tauri::Manager;
use tracing_subscriber::{fmt, layer::SubscriberExt, reload::Layer};

//Todo:
//  Add logging to rest of application
//  Config file for on/off on start
//  Client media control
//  application bug fixing -> bg img clipping

#[tokio::main]
async fn main() -> Result<()> {
    let (logger, reload) = Layer::new(None);

    let subscriber = tracing_subscriber::registry()
        .with(fmt::layer().with_writer(std::io::stdout))
        .with(logger);

    tracing::subscriber::set_global_default(subscriber)?;

    let state = AppState::init().await?;

    let (invoke_handler, register_events) = {
        let builder = tauri_specta::ts::builder()
            .commands(tauri_specta::collect_commands![
                commands::retrieve_data,
                commands::toggle_server,
                commands::kill_clients,
                commands::kill_client
            ])
            .events(tauri_specta::collect_events![AppEvent, LogEvent]);

        #[cfg(debug_assertions)]
        let builder = builder.path("../src/app/bindings.ts");

        builder.build().unwrap()
    };
    
    tauri::Builder::default()
        .manage(state)
        .invoke_handler(invoke_handler)
        .setup(move |app| {
            register_events(app);

            let window = app.get_webview_window("main").unwrap();
            window.set_shadow(true).unwrap();

            reload
                .modify(|x| *x = Some(AppLogLayer(app.handle().to_owned())))
                .unwrap();

            Ok(())
        })
        .build(tauri::generate_context!())?
        .run(|_app_handle, event| match event {
            _ => {}
        });

    Ok(())
}
