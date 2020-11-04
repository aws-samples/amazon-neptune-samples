// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import React, { useEffect } from "react";
import clsx from "clsx";
import axios from "axios";
import { makeStyles } from "@material-ui/core/styles";
import CssBaseline from "@material-ui/core/CssBaseline";
import Drawer from "@material-ui/core/Drawer";
import AppBar from "@material-ui/core/AppBar";
import Toolbar from "@material-ui/core/Toolbar";
import Typography from "@material-ui/core/Typography";
import Chatbot from "./Chatbot";
import Chart from "./Chart";
import configData from '../config.json';

const drawerWidth = 500;
const baseURL = configData['SERVER_URL'];

const useStyles = makeStyles((theme) => ({
  root: {
    display: "flex",
    fontFamily: "Amazon Ember, Roboto, Helvetica",
  },
  toolbar: {
    paddingRight: 24, // keep right padding when drawer closed
    backgroundColor: "#232F3E",
    textShadow: "0 1px 0 #000",
    color: "#eee",
    fontSize: "15px",
    fontWeight: "bold",
  },
  toolbarIcon: {
    display: "flex",
    alignItems: "center",
    justifyContent: "flex-end",
    padding: "0 8px",
    ...theme.mixins.toolbar,
  },
  appBar: {
    zIndex: theme.zIndex.drawer + 1,
    transition: theme.transitions.create(["width", "margin"], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.leavingScreen,
    }),
  },
  appBarShift: {
    marginLeft: drawerWidth,
    width: `calc(100% - ${drawerWidth}px)`,
    transition: theme.transitions.create(["width", "margin"], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
  },
  chartShift: {
    marginLeft: drawerWidth,
    marginTop: 64,
    width: `calc(100% - ${drawerWidth}px)`,
    height: `calc(100% - 64px)`,
    zIndex: 1000,
    border: "solid 1px #eeeeee",
    display: "inline-block",
  },
  menuButton: {
    marginRight: 36,
  },
  menuButtonHidden: {
    display: "none",
  },
  title: {
    flexGrow: 1,
    color: "#f2f2f2",
    fontFamily: "Amazon Ember",
  },
  appBarSpacer: theme.mixins.toolbar,
  content: {
    flexGrow: 1,
    height: "100vh",
    overflow: "auto",
  },
  drawer: {
    flexGrow: 1,
    height: "100%",
    overflow: "auto",
    width: drawerWidth,
  },
  container: {
    paddingTop: theme.spacing(4),
    paddingBottom: theme.spacing(4),
  },
  paper: {
    padding: theme.spacing(2),
    display: "flex",
    overflow: "auto",
    flexDirection: "column",
  },
  fixedHeight: {
    height: 240,
  },
  chatbot: {
    height: "100vh",
  },
}));

/**
 * The main function of the application
 */
export default function Layout() {
  const classes = useStyles();
  const [parameter, setSearchParameter] = React.useState(null);
  const [chartData, setChartData] = React.useState({ vertices: [], edges: [] });

  useEffect(() => {
    /**
     * This function merges the newData with the existing chartData
     * @param {*} newData - The new data to merge
     */
    const mergeChartData = (newData) => {
      console.log(newData);
      let data = { vertices: [], edges: [] };
      data.vertices = getUnique(
        chartData.vertices.concat(newData.vertices),
        "id"
      );
      data.edges = chartData.edges.concat(newData.edges);
      setChartData(data);
    };
    /** If the parameter has changed then make the approriate REST call */
    if (parameter) {
      if (parameter["author"] && parameter["tag"]) {
        axios
          .get(
            `${baseURL}/posts?authorname=${parameter["author"]}&topic=${parameter["tag"]}`
          )
          .then((repos) => {
            const data = repos.data;
            //console.log(data);
            mergeChartData(data);
          });
      } else if (parameter["author"]) {
        axios
          .get(`${baseURL}/posts?authorname=${parameter["author"]}`)
          .then((repos) => {
            const data = repos.data;
            //console.log(data);
            mergeChartData(data);
          });
      } else if (parameter["tag"]) {
        axios
          .get(`${baseURL}/posts?topic=${parameter["tag"]}`)
          .then((repos) => {
            const data = repos.data;
            //console.log(data);
            mergeChartData(data);
          });
      } else if (parameter["authorone"] && parameter["authortwo"]) {
        axios
          .get(
            `${baseURL}/authors?author1=${parameter["authorone"]}&author2=${parameter["authortwo"]}`
          )
          .then((repos) => {
            const data = repos.data;
            //console.log(data);
            mergeChartData(data);
          });
      }
    }
    // eslint-disable-next-line
  }, [parameter]);

  /** Returns the unique items in the array */
  const getUnique = (arr, comp) => {
    // store the comparison  values in array
    const unique = arr
      .map((e) => e[comp])

      // store the indexes of the unique objects
      .map((e, i, final) => final.indexOf(e) === i && i)

      // eliminate the false indexes & return unique objects
      .filter((e) => arr[e])
      .map((e) => arr[e]);

    return unique;
  };

  return (
    <div className={classes.root}>
      <CssBaseline />
      <AppBar position="absolute" className={clsx(classes.appBarShift)}>
        <Toolbar className={classes.toolbar}>
          <Typography
            component="h1"
            variant="h4"
            color="inherit"
            noWrap
            className={classes.title}
          >
            Amazon Blog Knowledge Graph
          </Typography>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        classes={{
          paper: clsx(classes.drawer),
        }}
      >
        <div className={clsx(classes.chatbot)}>
          <Chatbot setSearchParameter={setSearchParameter} />
        </div>
      </Drawer>
      <main className={classes.content}>
        <div id="chart" className={clsx(classes.chartShift)}>
          <Chart chartData={chartData} />
        </div>
      </main>
    </div>
  );
}
