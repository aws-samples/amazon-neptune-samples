// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import React, { useRef } from "react";
import ForceGraph2D from "react-force-graph-2d";
import { useWindowSize } from "@react-hook/window-size";
import PropTypes from "prop-types";

const nodeConfig = {
  author: {
    imgSrc:
      "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d8/Person_icon_BLACK-01.svg/50px-Person_icon_BLACK-01.svg.png",
    labelField: "name",
    tooltip: "name",
  },
  post: {
    imgSrc:
      "https://upload.wikimedia.org/wikipedia/commons/thumb/9/97/Document_icon_%28the_Noun_Project_27904%29.svg/50px-Document_icon_%28the_Noun_Project_27904%29.svg.png",
    labelField: "title",
    tooltip: "title",
  },
  tag: {
    imgSrc:
      "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Idea.svg/50px-idea.svg.png",
    labelField: "tag",
    tooltip: "tag",
  },
};

/**
 * Renders the network chart
 * @param {*} props - The props for the chart
 */
export default function Chart(props) {
  const fgRef = useRef();
  const chartData = {
    nodes: [],
    links: [],
  };
  const [width, height] = useWindowSize();
  if (props.chartData != null) {
    props.chartData["vertices"].forEach((node) => {
      chartData.nodes.push(JSON.parse(JSON.stringify(node)));
    });

    props.chartData["edges"].forEach((edge) => {
      chartData.links.push(JSON.parse(JSON.stringify(edge)));
    });
  }

  /**
   * Returns the height for the resized image
   * @param {*} node - The node object
   * @param {*} size - The size parameter
   */
  const getSizeY = (node, size) => {
    if (node.label === "author") {
      const ratio = node.img_height / node.img_width;
      return (
        size * (node.img_src === "" || node.img_src === undefined ? 1 : ratio)
      );
    } else {
      return size;
    }
  };

  /**
   * Returns the width for the resized image
   * @param {*} node - The node object
   * @param {*} size - The size parameter
   */
  const getSizeX = (node, size) => {
    if (node.label === "post") {
      const ratio = node.img_width / node.img_height;
      return (
        size * (node.img_src === "" || node.img_src === undefined ? 1 : ratio)
      );
    } else {
      return size;
    }
  };

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

  return (
    <ForceGraph2D
      height={height - 110}
      width={width - 550}
      ref={fgRef}
      graphData={chartData}
      nodeLabel="tooltip"
      linkDesc="label"
      cooldownTicks={1}
      linkDirectionalArrowLength={1.5}
      linkDirectionalArrowRelPos={1}
      d3VelocityDecay={0.001}
      onEngineStop={() => fgRef.current.zoomToFit(100, 25)}
      onNodeClick={(node) => {
        if (node.label === "post") {
          window.open(node.id);
        }
      }}
      onNodeDragEnd={(node) => {
        node.fx = node.x;
        node.fy = node.y;
      }}
      nodeCanvasObject={(node, ctx, globalScale) => {
        const label = node[nodeConfig[node.label].labelField];
        node.tooltip = node[nodeConfig[node.label].tooltip];
        const fontSize = 12 / globalScale;
        ctx.font = `${fontSize}px Sans-Serif`;

        let img = new Image();
        img.src =
          node.img_src === "" || node.img_src === undefined
            ? nodeConfig[node.label].imgSrc
            : node.img_src;
        const size = 8;
        ctx.drawImage(
          img,
          node.x - getSizeX(node, size) / 2,
          node.y - getSizeY(node, size) / 2,
          getSizeX(node, size),
          getSizeY(node, size)
        );

        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillStyle = "black";
        ctx.fillText(
          node.label === "post" ? truncateString(label, 30, "...") : label,
          node.x,
          node.y + getSizeY(node, size) / 2 + 1
        );
      }}
    />
  );
}

Chart.propTypes = {
  /** The function to call to set the search parameters */
  chartData: PropTypes.object,
};
