use parking_lot::Mutex;
use tracing::info;
use std::sync::Arc;
use tauri::{AppHandle, Manager};
use tauri_specta::Event;
use tokio_util::sync::CancellationToken;

use crate::{
    commands::AppEvent,
    media::{udp_broadcaster, websock_server},
    state::AppState,
};

pub async fn sink_manager(handle: AppHandle, token: CancellationToken) {
    info!("Starting sinks");

    let state = &handle.state::<Arc<Mutex<AppState>>>();

    let websock_server_task = tokio::spawn(websock_server(handle.clone()));
    let udp_broadcaster_task = tokio::spawn(udp_broadcaster());

    let websock_abort = websock_server_task.abort_handle();
    let udp_abort = udp_broadcaster_task.abort_handle();

    tokio::select! {
        _ = udp_broadcaster_task => AppEvent::UpdateData.emit(&handle).unwrap(),
        _ = websock_server_task => AppEvent::UpdateData.emit(&handle).unwrap(),
        _ = token.cancelled() => {}
    }
    udp_abort.abort();
    websock_abort.abort();

    let mut state = state.lock();
    state.kill_server();

    info!("Sinks stopped");
}
