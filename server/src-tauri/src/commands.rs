use std::sync::Arc;

use parking_lot::Mutex;
use serde::{Deserialize, Serialize};
use specta::Type as SpectaType;
use tauri::{AppHandle, State};
use tauri_specta::Event as SpectaEvent;

use crate::state::AppState;

#[derive(Debug, Clone, Serialize, Deserialize, SpectaType, SpectaEvent)]
pub enum AppEvent {
    UpdateData,
}

#[tauri::command]
#[specta::specta]
pub fn retrieve_data(state: State<Arc<Mutex<AppState>>>) -> (bool, Vec<String>) {
    let state = state.lock();
    let data = state.fetch_data();
    return data;
}

#[tauri::command]
#[specta::specta]
pub fn toggle_server(state: State<Arc<Mutex<AppState>>>, handle: AppHandle) {
    state.lock().toggle_server(handle);
}

#[tauri::command]
#[specta::specta]
pub fn kill_clients(state: State<Arc<Mutex<AppState>>>) {
    state.lock().kill_all_clients();
}

#[tauri::command]
#[specta::specta]
pub fn kill_client(state: State<Arc<Mutex<AppState>>>, client: String) {
    state.lock().kill_client(client)
}
