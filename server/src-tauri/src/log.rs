use std::collections::BTreeMap;

use serde::{Deserialize, Serialize};
use tauri::AppHandle;
use time::OffsetDateTime;
use tracing::{field::Visit, Subscriber};
use tracing_subscriber::Layer;

use specta::Type as SpectaType;
use tauri_specta::Event as SpectaEvent;

//This section was entirely finished on my 18th hour into the day
//Pray the code quality is fine
//I'll review it another day if I remember

pub struct AppLogLayer(pub AppHandle);

#[derive(Debug, Clone, Serialize, Deserialize, SpectaType, SpectaEvent)]
pub struct LogEvent {
    time: String,
    level: String,
    target: String,
    message: String,
}

impl<S: Subscriber> Layer<S> for AppLogLayer {
    fn on_event(
        &self,
        event: &tracing::Event<'_>,
        _ctx: tracing_subscriber::layer::Context<'_, S>,
    ) {
        let meta = event.metadata();
        let mut fields = AppLogVisitor(BTreeMap::new());
        event.record(&mut fields);
        let fields = fields.0;

        let time = OffsetDateTime::now_local().unwrap();

        let log = LogEvent {
            time: format!("{}T{}", time.date(), time.time()),
            level: meta.level().to_string(),
            target: meta.target().to_string(),
            message: fields.get("message").unwrap().to_owned(), //I think it always contains message?
        };

        log.emit(&self.0).unwrap();
    }
}

struct AppLogVisitor(BTreeMap<String, String>);

impl Visit for AppLogVisitor {
    fn record_debug(&mut self, field: &tracing::field::Field, value: &dyn std::fmt::Debug) {
        self.0.insert(field.to_string(), format!("{:?}", value));
    }
}
