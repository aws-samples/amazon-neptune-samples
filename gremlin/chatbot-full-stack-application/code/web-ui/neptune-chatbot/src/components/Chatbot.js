// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import React, { useEffect } from "react";
import Amplify from "aws-amplify";
import { AmplifyChatbot } from "@aws-amplify/ui-react";
import awsconfig from "../aws-exports";
import PropTypes from "prop-types";

Amplify.configure(awsconfig);

/**
 * This class handles the interaction with the Amazon Lex chatbot
 * @param {*} props - the props for this component
 */
export default function Chatbot(props) {
  useEffect(() => {
    const handleChatComplete = (event) => {
      const { data, err } = event.detail;
      console.log(data);
      if (data) {
        props.setSearchParameter(data["slots"]);
      }
      if (err) console.error("Chat failed:", err);
    };

    const chatbotElement = document.querySelector("amplify-chatbot");
    chatbotElement.addEventListener("chatCompleted", handleChatComplete);
    return function cleanup() {
      chatbotElement.removeEventListener("chatCompleted", handleChatComplete);
    };
  }, [props]);

  return (
    <AmplifyChatbot
      botName="blog_chatbot"
      botTitle=""
      welcomeMessage="What can I help you with?"
      style={{ "--height": "99vh" }}
      linkDirectionalArrowLength={1}
    />
  );
}

Chatbot.propTypes = {
  /** The function to call to set the search parameters */
  setSearchParameter: PropTypes.func,
};
