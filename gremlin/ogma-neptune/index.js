import Ogma from '@linkurious/ogma';
import * as api from './api';
import { createAutocomplete } from './ui';

const RED = 'rgba(229,77,159,0.78)';

const EdgeTypes = {
  follows: 'follows',
  likes: 'likes',
  tweets: 'tweets'
};

const NodeTypes = {
  user: 'User',
  tweet: 'Tweet'
};

const USER_ICON = '\uf007';
const TWEET_ICON = '\uf099';

const fontByType = {
  [NodeTypes.tweet]: 'Font Awesome 5 Brands',
  [NodeTypes.user]: 'Font Awesome 5 Free'
};

const iconByType = {
  [NodeTypes.tweet]: TWEET_ICON,
  [NodeTypes.user]: USER_ICON
};

const ogma = new Ogma({
  container: 'graph-container'
});

// Different styling for users and tweets (different icons)
ogma.styles.addRule({
  nodeAttributes: {
    radius: node => (node.getData('type') === NodeTypes.user ? 20 : 10),
    color: node => {
      const type = node.getData('type');
      if (type === NodeTypes.user) return '#ffa500';
      if (type === NodeTypes.tweet) return '#00bfff';
      return 'grey';
    },
    icon: {
      font: node => fontByType[node.getData('type')],
      content: node => iconByType[node.getData('type')],
      color: '#fff',
      minVisibleSize: 0
    }
  },
  // styling for different types of edges
  edgeAttributes: {
    color: edge => {
      const type = edge.getData('type');
      if (type === EdgeTypes.follows) return '#ffa500';
      if (type === EdgeTypes.tweets) return '#00bfff';
      if (type === EdgeTypes.likes) return RED;
      return 'grey';
    },
    text: {
      content: edge => edge.getData('type'),
      color: '#888'
    },
    shape: 'arrow',
    width: edge => {
      const type = edge.getData('type');
      if (type === EdgeTypes.follows) return 2;
      if (type === EdgeTypes.tweets) return 4;
      if (type === EdgeTypes.likes) return 5;
      return 1;
    }
  }
});

api.getNames().then(data => {
  const options = data.map(node => {
    const name = node.name;
    return {
      label: `<span>&nbsp;${name}</span>`,
      value: name
    };
  });
  createAutocomplete('#users', options, 20, evt => addUser(evt.text.value));
  // add a random user from the list not to have empty screen
  addUser(options[Math.floor(Math.random() * options.length)].value);
});

/**
 * @param {String} username
 */
function addUser(username) {
  api
    .getUser(username)
    .then(data => {
      ogma.addNodes(
        data.map(({ id, name, label }) => {
          return {
            id,
            attributes: {
              text: name[0],
              color: 'red'
            },
            data: { type: label }
          };
        })
      );
    })
    .then(() => ogma.layouts.force())
    .then(() => ogma.view.locateGraph());
}

// puts a user from the select on the graph
document.getElementById('user-select').addEventListener('submit', evt => {
  evt.preventDefault();
  const username = document.getElementById('users').value;
  addUser(username);
});

// gets the neighbours of a user
ogma.events.on('doubleclick', ({ target }) => {
  if (!target || !target.isNode) return;

  const id = target.getId();
  return expandNode(id, target.getData('type')).then(() =>
    ogma.layouts.force({ locate: true })
  );
});

/**
 * Gets neighbours of a user and likes of the tweets
 * @param {String} id
 * @returns
 */
function expandNode(id, type) {
  return getNeighbours(id)
    .then(() => getTweets(id))
    .then(() => {
      if (type === NodeTypes.tweet) return getLikes(id);
    });
}

/**
 * @param {string} tweetId
 */
function getLikes(tweetId) {
  return api.getLikes(tweetId).then(data => {
    ogma.addGraph({
      nodes: data.map(({ id, name, label }) => {
        return {
          id,
          attributes: {
            text: name[0]
          },
          data: { type: label }
        };
      }),
      edges: data.map(({ id }) => {
        return {
          id: `${id}-${tweetId}`,
          source: id,
          target: tweetId,
          data: {
            type: EdgeTypes.likes
          }
        };
      })
    });
  });
}

/**
 * @param {String} userId
 */
function getTweets(userId) {
  return api.getTweets(userId).then(data => {
    ogma.addGraph({
      nodes: data.map(({ id, text, label }) => {
        return {
          id,
          attributes: {
            text: text[0].substring(0, 50) + '...'
          },
          data: { type: label }
        };
      }),

      edges: data.map(({ id }) => {
        return {
          id: `${userId}-${id}`,
          source: userId,
          target: id,
          data: { type: EdgeTypes.tweets }
        };
      })
    });
  });
}

function getNeighbours(sourceNode) {
  return api.getNeighbours(sourceNode).then(data => {
    ogma.addGraph({
      nodes: data.map(({ id, name, label }) => {
        if (id !== sourceNode) {
          return {
            id,
            attributes: {
              text: name[0]
            },
            data: {
              type: label
            }
          };
        }
      }),

      edges: data.map(({ id }) => {
        return {
          id: `${sourceNode}-${id}`,
          source: id,
          target: sourceNode,
          data: { type: EdgeTypes.follows }
        };
      })
    });
  });
}
