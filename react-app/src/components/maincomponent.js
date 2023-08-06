import React from 'react';
import Home from './home';

import {BrowserRouter, Routes, Route} from 'react-router-dom';


function Main() {
    return (
        <div>
            <BrowserRouter>
                <Routes>
                    <Route exact path="/" element={<Home />} />
                    <Route to ='/'/>
                </Routes>
            </BrowserRouter>
        </div>
    );
}

export default Main;