import React from 'react';
import MetroTransitLogo from '../data/MetroTransitLogo.png';
    
// Styles and Material UI Imports
import { Grid, AppBar } from '@mui/material';


function Header({lastUpdateTime}) {
    return (
        <div style={{height: "10vh", width: "100vw"}}>
            <AppBar elevation={0} style={{ background: '#2E3B55' }}>
                <Grid container spacing={0}>
                    {/* ############################ Logo ########################### */}
                    <Grid item xs={4} sm={4} md={4}>
                        <img style={{maxHeight: "60px", margin: "10px"}} src={MetroTransitLogo} alt="logo" />
                    </Grid>
                    {/* ############################ Welcome Text ########################### */}
                    <Grid item xs={5} sm={5} md={5}>
                        <h1 style={{margin: "10px"}}>Madison Bus Tracking App</h1>
                    </Grid>
                    {/* ############################ Update Time ########################### */}
                    <Grid item xs={3} sm={3} md={3}>
                        <h4 style={{margin: "10px", textAlign: "right"}}>Last Updated: {lastUpdateTime === 0 ? "N/A" : lastUpdateTime[0] + " CT"}</h4>
                    </Grid>
                </Grid>
            </AppBar>
      </div>
    )
}

export default Header;