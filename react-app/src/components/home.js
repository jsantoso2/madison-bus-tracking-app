import React, {useState, useEffect, useRef} from 'react';

// Styles + Icons
import LocationOnIcon from '@mui/icons-material/LocationOn';
import Grid3x3Icon from '@mui/icons-material/Grid3x3';
import RouteIcon from '@mui/icons-material/Route';
import RssFeedIcon from '@mui/icons-material/RssFeed';
import DirectionsBusIcon from '@mui/icons-material/DirectionsBus';
import StartIcon from '@mui/icons-material/Start';

// Libraries
import Map, {Marker, Popup, Source, Layer} from 'react-map-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import Papa from 'papaparse';

import mapboxgl from "mapbox-gl"; // This is a dependency of react-map-gl even if you didn't explicitly install it

// Components
import Header from '../components/header';
import Slider from 'react-slick';
import 'slick-carousel/slick/slick.css'; 
import 'slick-carousel/slick/slick-theme.css';
import "../components/home.css";
import Alert from '@mui/material/Alert';

// Data
import StopTimes from '../data/stop_times.txt';
import Stops from '../data/stops.txt';
import Shapes from '../data/shapes.txt';

// Constants
const MAPBOX_API_TOKEN = '';
const WS_URL = "wss://websocket-docker-yb2eosnpwq-uc.a.run.app";

function Home() {
    // eslint-disable-next-line import/no-webpack-loader-syntax
    mapboxgl.workerClass = require("worker-loader!mapbox-gl/dist/mapbox-gl-csp-worker").default;

    // Slider Constants
    const settings = {
        dots: false, 
        infinite: true,
        speed: 500,
        slidesToShow: 10,
        slidesToScroll: 1,
        autoplay: true,
        autoplaySpeed: 10000      
    };

    // Initial Viewport
    const initialCoordinates = {
        longitude: -89.39619,
        latitude: 43.073273,
        zoom: 11.5
    };

    // Keeps Original Data State
    const [allStopsData, setAllStopsData] = useState([]);
    const [allStopTimesData, setAllStopTimesData] = useState([]);
    const [allShapesData, setAllShapesData] = useState([]);
    const [allBusses, setAllBusses] = useState({});
    const [allBussesArr, setAllBussesArr] = useState([]);

    // WebSocket State
    const [webSocket, setWebSocket] = useState([]);

    // Keeps Interactive State
    const [viewport, setViewPort] = useState(initialCoordinates)
    const [selectedStop, setSelectedStop] = useState(null);
    const [selectedBus, setSelectedBus] = useState(null);
    const [selectedRoute, setSelectedRoute] = useState(null);
    const mapRef = useRef();

    // Initial Render
    useEffect(() => {
        console.log("Initial Render");

        // Define initial websocket
        setWebSocket(new WebSocket(WS_URL));

        // Get All Stops Data
        fetch(Stops)
            .then((r) => r.text())
            .then((r) => 
                Papa.parse(r, {
                    header: true,
                    complete: function(r) {
                        setAllStopsData(r.data);
                    }
            }));
                
        // Get All StopTimes Data
        fetch(StopTimes)
            .then((r) => r.text())
            .then((r) => 
                Papa.parse(r, {
                    header: true,
                    complete: function(r) {
                        // Group By TripId
                        var result = r.data.reduce(function (r, a) {
                            r[a.trip_id] = r[a.trip_id] || [];
                            r[a.trip_id].push(a.stop_id);
                            return r;
                        }, Object.create(null));
                        setAllStopTimesData(result);
                    }
            }));
        
        // Get All Shapes Data
        fetch(Shapes)
            .then((r) => r.text())
            .then((r) => 
                Papa.parse(r, {
                    header: true,
                    complete: function(r) {
                        setAllShapesData(r.data);
                    }
            }));
    }, [])

    // WebSocket Data
    useEffect(() => {
        console.log("Connecting to Websocket!!");

        webSocket.onmessage = (msg) => {
            
                // keeps current state and update current state
                var parsed_msg = JSON.parse(msg.data);
                var temp = allBusses;
                var temp_arr = [];
                temp[parsed_msg.id] = parsed_msg;

                // Delete key if > 5min updatetime
                for (const key in temp){
                    if (Date.parse(new Date().toLocaleString('en-US', {timeZone: 'America/Chicago'})) - Date.parse(temp[key].updateTime) > 300000){
                        delete temp[key];
                    } else {
                        temp_arr.push(temp[key]);
                    }
                }
                
                // Set New Status Busses
                setAllBusses(temp);
                setAllBussesArr(Object.keys(temp).map((key) => temp[key].id));
                
                //console.log("allBusses", allBusses);
        }

        webSocket.onclose = function () { webSocket.close(); }; // disable onclose handler first

    }, [webSocket, allBusses]);
    

    // Helper method to setSelectedRoute
    function helperSetSelectedRoute(busAttr){
        var temp = allShapesData.filter(x => +x.shape_id === +busAttr.shapeId).map(x => {return [+x.shape_pt_lon, +x.shape_pt_lat]});

        setSelectedRoute({
                    type: "Feature",
                    properties: {},
                    geometry: {
                    type: "LineString",
                    coordinates: temp
                    }
                });
    }

    // Helper method to return stop Marker color
    function helperReturnStopMarkerColor(stop_id){
        if (allStopTimesData === [] || allStopTimesData === null || stop_id === null || selectedBus === null || selectedRoute === null){
            return "orange";
        }

        return (allStopTimesData[selectedBus.tripId].includes(stop_id)) ? "#27AE60" : "orange";
    }

    // Helper Method to reset selections
    function resetSelection(){
        setViewPort(initialCoordinates);
        setSelectedStop(null);
        setSelectedBus(null);
        setSelectedRoute(null);
        mapRef.current?.flyTo({center: [initialCoordinates.longitude, initialCoordinates.latitude], duration: 1000, zoom: initialCoordinates.zoom});
    }
    
    //style={{background: ((selectedBus !== null) && (allBusses[k].id === selectedBus.id))? "#27AE60" : "white" , height: "18vh", margin: "5px", fontWeight: "bold", fontSize: "3px", textAlign: "center"}}
    return (
        <div>
            <Header lastUpdateTime = {Object.keys(allBusses).map((k) => allBusses[k].updateTime)}/>
            <div style={{height: "22vh", width: "100vw", backgroundColor: "black"}}> 
                <div style={{display: "flex", alignItems: "center", justifyContent: "center", marginBotton: "5px", marginTop: "15px"}}>
                    <button style={{backgroundColor: "#888484"}} onClick={(e) => {e.preventDefault(); resetSelection();}}>RESET</button>
                </div>
                <div>
                    {Object.keys(allBusses).length > 0 ? null : 
                        <Alert severity="info" variant="filled" style={{height: "40px", margin: "10px", alignItems: "center"}}>Waiting for next available message!!</Alert>}
                </div>
                <Slider {...settings}>
                    {allBusses ? (Object.keys(allBusses).sort().map((k) => (
                        <div key={"card_busses_" + allBusses[k].id}
                             onClick={(e) => {e.preventDefault(); setSelectedBus(allBusses[k]); helperSetSelectedRoute(allBusses[k]);}}
                        >
                            <table style={{width: "150px", height: "17vh", background: ((selectedBus !== null) && (allBusses[k].id === selectedBus.id))? "#27AE60" : "#888484"}}>
                                <tbody>
                                <tr>
                                    <td><Grid3x3Icon style={{width: "12px", height: "12px", background: "none"}} /></td>
                                    <td>{allBusses[k].id}</td>
                                </tr>
                                <tr>
                                    <td><RouteIcon style={{width: "12px", height: "12px", background: "none"}} /></td>
                                    <td>{allBusses[k].routeName}</td>
                                </tr>
                                <tr>
                                    <td><StartIcon style={{width: "12px", height: "12px", background: "none"}} /></td>
                                    <td>{allBusses[k].startTime}</td>
                                </tr>
                                <tr>
                                    <td><DirectionsBusIcon style={{width: "12px", height: "12px", background: "none"}} /></td>
                                    <td style={{fontSize: "9px"}}>{allBusses[k].tripHeadSign}</td>
                                </tr>
                                <tr>
                                    <td><RssFeedIcon style={{width: "12px", height: "12px", background: "none"}} /></td>
                                    <td style={{fontSize: "8.5px"}}>{allBusses[k].nextStopName}</td>
                                </tr>
                                <tr>
                                    <td><RssFeedIcon style={{width: "12px", height: "12px", background: "none"}} /></td>
                                    <td>{Math.ceil(allBusses[k].nextStopApproachSec / 60) + " min"}</td>
                                </tr>
                                </tbody>
                            </table>
                        </div>
                    ))) : null}
                </Slider>
            </div>
            <div>
                <Map
                    ref={mapRef}
                    mapboxAccessToken={MAPBOX_API_TOKEN}
                    initialViewState={viewport}
                    style={{width: "100vw", height: "65vh"}}
                    mapStyle="mapbox://styles/jsantoso2/clk962xgt036q01nmeac9365i"
                    reuseMaps={true}
                >
                                    
                    {allStopsData.map(x => (
                        <Marker key={"stops_" + x.stop_id} latitude={x.stop_lat} longitude={x.stop_lon}>
                            <LocationOnIcon onClick={(e) => {e.preventDefault(); setSelectedStop(x);}} 
                                style={{color: helperReturnStopMarkerColor(x.stop_id), width: "10px", height: "10px", background: "none", border: "none", "cursor": "pointer"}}/>
                        </Marker>
                    ))}
                    
                    {allBusses ? (Object.keys(allBusses).map((k) => (
                        <Marker key={"busses_" + allBusses[k].id} latitude={allBusses[k].latitude} longitude={allBusses[k].longitude}>
                            <div style={{color: "white", background: ((selectedBus !== null) && (allBusses[k].id === selectedBus.id))? "#27AE60" : "#0000FF", border: "none", "cursor": "pointer", fontWeight: "bold", width: "20px", height: "20px", textAlign: "center"}}
                                onClick={(e) => {e.preventDefault(); setSelectedBus(allBusses[k]); helperSetSelectedRoute(allBusses[k]);}}>
                                {allBusses[k].routeName}
                            </div>
                        </Marker>
                    ))) : null}

                    {selectedBus ? 
                        (<div>
                        <Source id="selectedRoutePath" type="geojson" data={selectedRoute}>
                            <Layer id="lineLayer" type="line" source="data"
                                layout={{"line-join": "round", "line-cap": "round", "visibility":  selectedBus ? "visible" : "none"}}
                                paint={{"line-color": "red", "line-width": 3}} />
                        </Source>
                        <Popup key={"selectedBusId_" + selectedBus.id} latitude={+selectedBus.latitude} longitude={+selectedBus.longitude}
                                        closeOnClick={false} onClose={() => {}}>
                                        <div>{"BusId: " + selectedBus.id}</div>
                                        <div>{"TripSign: " + selectedBus.tripHeadSign}</div>
                        </Popup>
                        </div>
                 
                        ) : null}

                    {selectedStop ? (
                        <Popup key={"selectedStopId_" + selectedStop.stop_id} latitude={+selectedStop.stop_lat} longitude={+selectedStop.stop_lon}
                               closeOnClick={false} onClose={() => {setSelectedStop(null);}}
                        >
                            <div>{"StopId: " + selectedStop.stop_id}</div>
                            <div>{"StopName: " + selectedStop.stop_name}</div>
                        </Popup>
                    ) : null}

                </Map>
            </div>
        </div>
    )
}

export default Home;
