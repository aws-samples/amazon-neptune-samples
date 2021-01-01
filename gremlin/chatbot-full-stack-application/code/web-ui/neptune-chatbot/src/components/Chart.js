// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import React from "react";
import { useWindowSize } from "@react-hook/window-size";
import coseBilkent from 'cytoscape-cose-bilkent';
import PropTypes from "prop-types";
import CytoscapeComponent from 'react-cytoscapejs';
import Cytoscape from 'cytoscape';

Cytoscape.use(coseBilkent);

/**
 * Renders the network chart
 * @param {*} props - The props for the chart
 */
export default function Chart(props) {
  let cy;
  const chartData = [];
  const [width, height] = useWindowSize();

  /**
   * Retieves the proper image for the  label
   * @param {*} label The image URL
   */
   const getDefaultImage = (label) => {
    switch (label) {
      case 'author':
        return "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d8/Person_icon_BLACK-01.svg/50px-Person_icon_BLACK-01.svg.png";
      case 'post':
        return "https://upload.wikimedia.org/wikipedia/commons/thumb/9/97/Document_icon_%28the_Noun_Project_27904%29.svg/50px-Document_icon_%28the_Noun_Project_27904%29.svg.png";
      case 'tag':
        return "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Idea.svg/50px-idea.svg.png";
    }
  }

  /**
   * Returns the label text for the node
   * @param {*} node the node object
   */
  const getLabelText = (node) => {
    switch (node.label) {
      case 'author':
        return node.name;
      case 'post':
        return truncateString(node.title, 50, "...");
      case 'tag':
        return node.tag;
    }
  }

  /**
   * Returns the proper background color based on the label
   * @param {*} label The label
   */
  const getBackgroundColor = (label) => {
    switch (label) {
      case 'author':
        return "#48a3c6";
      case 'post':
        return "#e47911";
      case 'tag':
        return "#cccccc";
      default:
        return "white";
    }
  }

  /**
   * Truncates the string to specified length
   * @param {*} str The string to truncate
   * @param {*} len The length to truncate it to
   * @param {*} append If you want to append anything to the end
   */
  const truncateString = (str, len, append) => {
    var newLength;
    append = append || ""; //Optional: append a string to str after truncating. Defaults to an empty string if no value is given

    if (append.length > 0) {
      append = " " + append; //Add a space to the beginning of the appended text
    }
    if (str.indexOf(" ") + append.length > len) {
      return str; //if the first word + the appended text is too long, the function returns the original String
    }

    str.length + append.length > len
      ? (newLength = len - append.length)
      : (newLength = str.length); // if the length of original string and the appended string is greater than the max length, we need to truncate, otherwise, use the original string

    var tempString = str.substring(0, newLength); //cut the string at the new length
    tempString = tempString.replace(/\s+\S*$/, ""); //find the last space that appears before the substringed text

    if (append.length > 0) {
      tempString = tempString + append;
    }
    return tempString;
  };

  if (props.chartData != null) {
    props.chartData["vertices"].forEach((node) => {
      let d = {data: (JSON.parse(JSON.stringify(node)))}
      d.data.label = getLabelText(node)
      if(node.thumbnail) {
        d.style={width: node.img_width, height: node.img_height, shape: 'roundrectangle'}
        d.style["background-color"]="transparent"      
        d.style["background-image"]=`url(${node.thumbnail})`
      } else {
        d.style={width: 75, height: 75, shape: 'roundrectangle'}
        d.style["background-color"]=getBackgroundColor(node.label);
        d.style["background-image"]=getDefaultImage(node.label);
      }
      d.style["background-fit"] = "cover cover"

      chartData.push(d);
    });

    props.chartData["edges"].forEach((edge) => {
      let d = {data: (JSON.parse(JSON.stringify(edge)))}
      chartData.push(d);
    });
  }

  const style={height: height - 75, width: width - 510}
  const layout = { name: 'cose-bilkent',
      ready: function () {
      },
      stop: function () {
      },
      quality: 'default',
      nodeDimensionsIncludeLabels: true,
      refresh: 30,
      fit: true,
      padding: 10,
      randomize: true,
      nodeRepulsion: 450000,
      idealEdgeLength: 100,
      edgeElasticity: 0.45,
      nestingFactor: 0.1,
      gravity: 0.1,
      numIter: 2500,
      tile: true,
      animate: 'end',
      animationDuration: 500,
      tilingPaddingVertical: 10,
      tilingPaddingHorizontal: 10,
      gravityRangeCompound: 1.5,
      gravityCompound: 1.0,
      gravityRange: 3.8,
      initialEnergyOnIncremental: 0.5
    };
  console.log(chartData);
  return (
    <CytoscapeComponent style={style}
      elements={chartData}
      layout={layout}
      cy={(c) => { cy = c }}
    />

  );
}

Chart.propTypes = {
  /** The function to call to set the search parameters */
  chartData: PropTypes.object,
};
