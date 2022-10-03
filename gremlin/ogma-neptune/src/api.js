/**
 * @description
 * This file is a mock API to your endpoints for Neptune Gremlin. It just uses
 * cached responses from the Gremlin implementation that you can
 * find in `indexLambda.js` serverless application.
 */

let cache = {};

const ready = fetch("cache.json")
  .then((response) => response.json())
  .then((data) => {
    cache = data;
  });

const PROXY_API_URL =
  "https://vr6cx4jz7i.execute-api.eu-west-1.amazonaws.com/test";

export function connect() {
  return ready;
}

/**
 * @param {string} url
 * @returns {Promise<any>}
 */
export function get(url) {
  //return ready.then(() => cache[url]);
  return fetch(PROXY_API_URL + url).then((r) => r.json());
}

/**
 * @param {String} tweetId
 * @returns {Promise<any>}
 */
export function getLikes(tweetId) {
  return get(`/whichusersliketweet?tweetid=${tweetId}`);
}
/**
 * @typedef {object} NameRecord
 * @property {string} name
 */
/**
 * @returns {Promise<NameRecord[]>}
 */
export function getNames() {
  return get("/initialize");
}

/**
 * @typedef {object} UserRecord
 * @property {string[]} name
 * @property {string[]} notification
 * @property {string[]} usercity
 * @property {Date[]} RegisteredDate
 * @property {string} id
 * @property {string} label
 */

/**
 * @param {String} sourceNode
 * @returns {Promise<UserRecord[]>}
 */
export function getNeighbours(sourceNode) {
  return get(`/neighbours?id=${sourceNode}`);
}

/**
 * Retrieves user by name
 * @param {string} username
 * @returns {Promise<UserRecord[]>}
 */
export function getUser(username) {
  const lastchar = username.substring(username.length - 1, username.length);
  const nextletter = String.fromCharCode(lastchar.charCodeAt(0) + 1);
  const touser = username.substring(0, username.length - 1) + nextletter;

  return get(`/search?username=${username}&touser=${touser}`);
}

/**
 * @typedef {object} TweetRecord
 * @property {string} id
 * @property {string[]} text
 * @property {string} label
 */

/**
 * @param {String} userId
 * @returns {Promise<Tweet[]>}
 */
export function getTweets(userId) {
  return get(`/getusertweets?userid=${userId}`);
}
