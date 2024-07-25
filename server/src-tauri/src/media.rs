use std::{collections::HashMap, net::SocketAddr, sync::Arc, time::Duration};

use color_eyre::Result;
use futures::SinkExt;
use parking_lot::Mutex;
use serde::Serialize;
use tauri::{AppHandle, Manager};
use tauri_specta::Event;
use tokio::net::{TcpListener, TcpStream, UdpSocket};
use tokio_tungstenite::{tungstenite::Message, WebSocketStream};
use tokio_util::sync::CancellationToken;
use tracing::{error, info};
use windows::{
    Media::Control::{
        GlobalSystemMediaTransportControlsSession,
        GlobalSystemMediaTransportControlsSessionMediaProperties,
    },
    Storage::Streams::DataReader,
};

use crate::{commands::AppEvent, state::AppState};

pub async fn udp_broadcaster() -> Result<()> {
    let udp = UdpSocket::bind("0.0.0.0:0").await?;
    udp.set_broadcast(true)?;

    info!("UDP broadcaster started -> {}", udp.local_addr()?);

    loop {
        udp.send_to(&[80, 80, 80, 80], "255.255.255.255:45777")
            .await?;
        tokio::time::sleep(Duration::from_millis(500)).await;
    }
}

pub async fn websock_server(handle: AppHandle) -> Result<()> {
    let socket: SocketAddr = "0.0.0.0:45777".parse()?;
    let listener = TcpListener::bind(socket).await?;
    info!("TCP listener running -> {}", listener.local_addr()?);

    loop {
        let (stream, socket) = listener.accept().await?;
        let websocket = tokio_tungstenite::accept_async(stream).await?;
        tokio::spawn(handler(handle.clone(), socket, websocket));
    }
}

async fn handler(
    handle: AppHandle,
    host: SocketAddr,
    mut socket: WebSocketStream<TcpStream>,
) -> Result<()> {
    info!("{} -> Connection opened", host.to_string());

    let state = handle.state::<Arc<Mutex<AppState>>>();
    let mut state = state.lock();

    let token = CancellationToken::new();
    state.add_client(host.to_string(), token.clone());
    AppEvent::UpdateData.emit(&handle)?;

    let gsmt = state.clone_gmst();

    //Explicitly call Drop on the mutex to unlock so the program does not deadlock
    drop(state);

    let handler_loop = || async move {
        let mut previous_song_map: HashMap<String, String> = HashMap::new();

        loop {
            let sessions = gsmt
                .GetSessions()?
                .into_iter()
                .collect::<Vec<GlobalSystemMediaTransportControlsSession>>();

            let mut mapped_sessions: Vec<MusicInfo> = vec![];

            for session in sessions {
                let session_id = session.SourceAppUserModelId()?.to_string();

                if !previous_song_map.contains_key(&session_id) {
                    previous_song_map.insert(session_id.clone(), "".to_string());
                }

                let previous_song = previous_song_map.get_mut(&session_id).unwrap();

                match get_session_details(&session, previous_song).await {
                    Ok(session) => mapped_sessions.push(session),
                    Err(err) => {
                        error!("Session map error -> {}", err)
                    }
                }
            }

            socket
                .send(Message::Text(serde_json::to_string(&mapped_sessions)?))
                .await?;

            std::thread::sleep(std::time::Duration::from_millis(100))
        }

        #[allow(unreachable_code)]
        //Ok -> added for type checking, will never reach because looped above
        Ok::<(), color_eyre::Report>(())
    };

    tokio::select! {
        handler_result = handler_loop() => {
            let handler_exit_error = handler_result.err().unwrap();

            let state = handle.state::<Arc<Mutex<AppState>>>();
            let mut state = state.lock();
            state.kill_client(host.to_string());

            error!(
                "{} -> Connection closed -> {}",
                host.to_string(),
                handler_exit_error.to_string()
            );
        }
        _ = token.cancelled() => {
            info!(
                "{} -> Connection closed -> Cancelled",
                host.to_string()
            );
        }
    }
    AppEvent::UpdateData.emit(&handle)?;

    Ok(())
}

async fn get_session_details(
    session: &GlobalSystemMediaTransportControlsSession,
    previous_song: &mut String,
) -> Result<MusicInfo> {
    let session_info = session.TryGetMediaPropertiesAsync()?.await?;
    let session_timeline = session.GetTimelineProperties()?;

    let start_time = Duration::from(session_timeline.StartTime()?).as_millis() as u64;
    let end_time = Duration::from(session_timeline.EndTime()?).as_millis() as u64;
    let position = Duration::from(session_timeline.Position()?).as_millis() as u64;

    let song = session_info.Title()?.to_string();

    let (artwork, has_changed) = {
        let has_changed = &song != previous_song;

        let artwork = if has_changed {
            *previous_song = song.clone();
            get_thumbnail(&session_info)?
        } else {
            vec![]
        };

        (artwork, has_changed)
    };

    let music_info = MusicInfo {
        source_id: session.SourceAppUserModelId()?.to_string(),
        song_name: song,
        artist: session_info.Artist()?.to_string(),
        album: session_info.AlbumTitle()?.to_string(),
        start_time,
        end_time,
        position,
        playing: match session.GetPlaybackInfo()?.PlaybackStatus()? {
            windows::Media::Control::GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing => true,
            _ => false,
        },
        artwork,
        artwork_changed: has_changed
    };

    Ok(music_info)
}

fn get_thumbnail(
    session_info: &GlobalSystemMediaTransportControlsSessionMediaProperties,
) -> Result<Vec<u8>> {
    let thumb = session_info.Thumbnail()?.OpenReadAsync()?.get()?;
    let stream_len = thumb.Size()? as usize;
    let mut data = vec![0u8; stream_len];

    let reader = DataReader::CreateDataReader(&thumb)?;
    reader.LoadAsync(stream_len as u32)?.get()?;

    reader.ReadBytes(&mut data)?;

    reader.Close()?;
    thumb.Close()?;

    Ok(data)
}

#[derive(Debug, Serialize)]
struct MusicInfo {
    source_id: String,
    song_name: String,
    artist: String,
    album: String,
    start_time: u64,
    end_time: u64,
    position: u64,
    playing: bool,
    artwork: Vec<u8>,
    artwork_changed: bool,
}
