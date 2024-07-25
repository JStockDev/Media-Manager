use color_eyre::Result;
use parking_lot::Mutex;
use std::{collections::HashMap, sync::Arc};
use tauri::AppHandle;
use tokio_util::sync::CancellationToken;
use windows::Media::Control::GlobalSystemMediaTransportControlsSessionManager;

use crate::tasks::sink_manager;

pub struct AppState {
    gsmt: GlobalSystemMediaTransportControlsSessionManager,
    sink_thread: Option<CancellationToken>,
    current_clients: HashMap<String, CancellationToken>,
}

impl AppState {
    pub async fn init() -> Result<Arc<Mutex<AppState>>> {
        let state = Arc::new(Mutex::new(AppState {
            gsmt: GlobalSystemMediaTransportControlsSessionManager::RequestAsync()?.await?,
            sink_thread: None,
            current_clients: HashMap::new(),
        }));

        return Ok(state);
    }

    pub fn fetch_data(&self) -> (bool, Vec<String>) {
        let server_state = self.sink_thread.is_some();
        let clients = self
            .current_clients
            .keys()
            .map(|client| client.to_owned())
            .collect();

        return (server_state, clients);
    }

    pub fn toggle_server(&mut self, handle: AppHandle) {
        if let Some(token) = &self.sink_thread {
            token.cancel();
            self.kill_all_clients();

            self.sink_thread = None;
        } else {
            let token = CancellationToken::new();
            tokio::spawn(sink_manager(handle.clone(), token.clone()));

            self.sink_thread = Some(token);
        }
    }

    pub fn kill_server(&mut self) {
        //In the context this is called, the option should always have a token inside
        //I also haven't tested all the edge cases so :\ no clue if the intended behaviour always occurs
        if let Some(token) = &self.sink_thread {
            token.cancel();
            self.kill_all_clients();

            self.sink_thread = None;
        }
    }

    pub fn add_client(&mut self, hostname: String, token: CancellationToken) {
        self.current_clients.insert(hostname, token);
    }

    pub fn clone_gmst(&self) -> GlobalSystemMediaTransportControlsSessionManager {
        self.gsmt.clone()
    }

    pub fn kill_client(&mut self, hostname: String) {
        if let Some(token) = self.current_clients.remove(&hostname) {
            token.cancel();
        }
    }

    pub fn kill_all_clients(&mut self) {
        for (_, token) in &self.current_clients {
            token.cancel();
        }

        self.current_clients.clear();
    }
}
