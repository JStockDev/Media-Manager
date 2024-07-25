"use client"

import invader from '#/public/icon.png';
import { event } from '@tauri-apps/api';

import { LogEvent, commands, events } from "./bindings";

import { WebviewWindow } from '@tauri-apps/api/webviewWindow';
import { useEffect, useState } from "react";

export default function Home() {

  const [maxIcon, setMaxIcon] = useState("\uE922");

  const [serverState, setServerState] = useState(false);
  const [openClients, setOpenClients] = useState<string[]>([]);

  const [logs, setLogs] = useState<LogEvent[]>([]);

  useEffect(() => {
    const window = WebviewWindow.getCurrent()

    const listener = WebviewWindow.getCurrent().onResized(async () => {
      await window.isMaximized() ? setMaxIcon("\uE923") : setMaxIcon("\uE922");
    })

    return () => {
      listener.then((listener) => listener());
    }
  }, []);

  useEffect(() => {
    let event = events.logEvent.listen((data) => {
      setLogs((prev) => [...prev, data.payload]);
    });

    return () => {
      event.then((event) => event());
    }
  }, [logs])

  useEffect(() => {
    let event = events.appEvent.listen(async (data) => {
      switch (data.payload) {
        case 'UpdateData':
          await retrieveData();
          break;
      }
    });

    return () => {
      event.then((event) => event());
    }
  }, [serverState, openClients])

  useEffect(() => {
    retrieveData();
  }, [serverState, openClients])

  async function retrieveData() {
    let data = await commands.retrieveData();

    setServerState(data[0])
    setOpenClients(data[1])
  }

  return (
    <div className="bg-black w-screen h-screen flex flex-col">
      <div data-tauri-drag-region className="w-full h-8 bg-content flex justify-between">
        <div className='flex items-center justify-center ml-2'>
          {/* Static export prevents Image optimisation, so default tag*/}
          {/* eslint-disable-next-line @next/next/no-img-element*/}
          <img src={invader.src} alt='invader' className='aspect-square h-3/5' />
        </div>
        <div className="flex justify-evenly">
          <div onClick={() => WebviewWindow.getCurrent().minimize()} className="h-full w-[45px] font-icons flex justify-center items-center text-body text-[11px] duration-150 hover:bg-hover">&#xE921;</div>
          <div onClick={() => WebviewWindow.getCurrent().toggleMaximize()} className="h-full w-[45px] font-icons flex justify-center items-center text-body text-[11px] duration-150 hover:bg-hover">{maxIcon}</div>
          <div onClick={() => WebviewWindow.getCurrent().close()} className="h-full w-[45px] font-icons flex justify-center items-center text-body text-[11px] duration-150 hover:bg-[#e81123]">&#xE8BB;</div>
        </div>
      </div>

      <div className='w-full h-[calc(100vh-32px)] flex flex-col items-center justify-evenly bg-background text-body'>
        <div className='flex items-center justify-center text-4xl font-bold'>Media Manager</div>
        <div className='h-2/6 w-11/12 flex justify-between items-center'>
          <div className='h-full w-[48%] bg-content border-2 border-neutral-600 rounded-xl flex flex-col items-center justify-evenly'>
            <div className='text-2xl font-bold text-center'>Settings</div>
            <button
              onClick={() => commands.toggleServer().then(() => retrieveData())}
              className={`w-11/12 h-10 p-3 bg-background rounded-full duration-200 border-2 ${serverState ? "border-green-900" : "border-red-900"} flex items-center justify-center`}>Start the sink and run the stream</button>
            <button
              onClick={() => commands.killClients().then(() => retrieveData())}
              className='w-11/12 h-10 p-3 bg-background rounded-full border-2 border-neutral-600 flex items-center justify-center'>Kill all current clients</button>
          </div>
          <div className='h-full w-[48%] bg-content border-2 border-neutral-600 rounded-xl flex flex-col justify-evenly items-center'>
            <div className='text-2xl font-bold text-center'>Connected Clients</div>
            <div className='h-4/6 w-full overflow-scroll scrollbar-thin scrollbar-thumb-background scrollbar-thumb-rounded-full'>
              {
                openClients.map((client) =>

                  <div key={client} className='w-full flex justify-between items-center px-3'>
                    <div>{client}</div>
                    <button onClick={() => commands.killClient(client).then(() => retrieveData())} className='font-icons text-sm'>&#xE8BB;</button>
                  </div>
                )
              }
            </div>
          </div>
        </div>
        <div className='h-2/6 w-11/12 bg-content border-2 border-neutral-600 rounded-xl flex flex-col justify-evenly items-center'>
          <div className='text-2xl font-bold'>Logs</div>

          <div className='h-4/6 w-full overflow-scroll scrollbar-thin scrollbar-thumb-background scrollbar-thumb-rounded-full '>
            {
              logs.map((log) => {

                let level_colour = '';

                switch (log.level) {
                  case "ERROR":
                    level_colour = 'text-red-400';
                    break;
                  case "WARN":
                    level_colour = 'text-orange-400';
                    break;

                  case "INFO":
                    level_colour = 'text-green-400';
                    break;

                  case "DEBUG":
                    level_colour = 'text-blue-400';
                    break;  
                  case "TRACE":
                    level_colour = 'text-purple-400';
                    break;

                  default:
                    break;

                }

                return (
                  <div key={log.message} className='w-max flex items-center px-3 font-logs text-sm'>
                    <div className='text-gray-500 letter'>{log.time}</div>
                    <div className={`ml-2 ${level_colour}`}>{log.level}</div>
                    <div className='ml-2'>{log.target}</div>
                    <div className='ml-2'>{log.message}</div>
                  </div>
                )
              }
              )
            }
          </div>
        </div>
      </div>
    </div>
  );
}

