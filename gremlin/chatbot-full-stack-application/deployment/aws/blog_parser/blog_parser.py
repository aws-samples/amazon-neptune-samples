# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0
from requests import get
from requests.exceptions import RequestException
from contextlib import closing
from bs4 import BeautifulSoup
import sys
import json
import argparse
import logging


class AwsBlogParser:
    # Global Variables
    # Array containing the HTML of each blogs paginated blog post titles and previews

    logger = logging.getLogger()

    def __init__(self, url: str):
        super().__init__()
        self.blog_url = url

    def __simple_get(self, url):
        """
        Attempts to get the content at `url` by making an HTTP GET request.
        If the content-type of response is some kind of HTML/XML, return the
        text content, otherwise return None.
        """
        try:
            with closing(get(url, stream=True)) as resp:
                if self.__is_good_response(resp):
                    return resp.content
                else:
                    return None

        except RequestException as e:
            self.logger.error(
                'Error during requests to {0} : {1}'.format(url, str(e)))
            return None

    def __is_good_response(self, resp):
        """
        Returns True if the response seems to be HTML, False otherwise.
        """
        content_type = resp.headers['Content-Type'].lower()
        return (resp.status_code == 200
                and content_type is not None
                and content_type.find('html') > -1)

    def parse(self):
        siteContent = []
        blogPosts = []  # Array containing the URLs for each blog post
        output = {}  # JSON document containing all posts, author, data published, and tags
        blog_url = ""
        newBlog = self.blog_url
        siteContent.append(str(self.__simple_get(newBlog)))
        # Starting with the second page of Older Post, pull the contents of each subsequent page into an array
        pageNumber = 2
        siteURL = newBlog + 'page/' + str(pageNumber) + '/'
        nextSiteContent = self.__simple_get(siteURL)
        while (nextSiteContent != 'None'):
            siteContent.append(nextSiteContent)
            pageNumber += 1
            siteURL = newBlog + 'page/' + str(pageNumber) + '/'
            nextSiteContent = str(self.__simple_get(siteURL))

        self.logger.info("Number of pages found: " + str(len(siteContent)))

        # Take each page of the blog contents and parse out the URL for each separate blog post
        for page in siteContent:
            html = BeautifulSoup(page, 'html.parser')
            post_images = html.select('article[class="blog-post"] img[src]')
            Urls = html.select('h2[class="blog-post-title"] a[href]')
            for i, url in enumerate(Urls):
                blogPosts.append(
                    {'url': url.get('href'),
                     'img_src': post_images[i]['src'],
                     'img_height': post_images[i]['height'],
                     'img_width': post_images[i]['width']
                     }
                )

        self.logger.info("Number of blog posts found: " + str(len(blogPosts)))
        # Using the URLs for each of the posts - contained in blogPosts[] - collect the HTML for the post site
        # then parse the contents.  Return Author, Date, Tags, and Post Contents in a JSON.
        # declare a new array of posts within the output JSON document.
        output['posts'] = []
        for post in blogPosts:
            self.logger.info("Processing post at: " + post['url'])
            postHtml = BeautifulSoup(
                self.__simple_get(post['url']), 'html.parser')
            Authors = postHtml.select('span[property="author"]')
            Title = postHtml.select('h1[property="name headline"]')
            DatePublished = postHtml.select('time[property="datePublished"]')
            Categories = postHtml.select('span[property="articleSection"]')
            postContent = postHtml.select('section[property="articleBody"]')
            # Extract the author images from the HTML body
            author_images = []
            author_section = None
            for section in postHtml.findAll('h3'):
                if section.next == "About the Authors" or section.next == "About the Author":
                    author_section = section
                    break
            if author_section is not None:
                for tag in author_section.next_siblings:
                    if not str(type(tag)) == "<class 'bs4.element.NavigableString'>":
                        children = tag.findChildren('img')
                        if len(children) == 1:
                            author_images.append({'url': children[0]['src'],
                                                  'img_height': children[0]['height'] if children[0].has_attr('height') else 100,
                                                  'img_width': children[0]['width']} if children[0].has_attr('width') else 200,)

            tagArray = []
            authorArray = []
            for tag in Categories:
                tagArray.append(tag.text)
            for auth in Authors:
                authorArray.append(auth.text)
            postJson = {}
            postJson["url"] = post["url"]
            postJson["img_src"] = post["img_src"]
            postJson["img_height"] = post["img_height"]
            postJson["img_width"] = post["img_width"]
            postJson["title"] = Title[0].text
            authors = []
            i = 0
            for i, a in enumerate(authorArray):
                auth = {'name': a}
                if i < len(author_images):
                    auth['img_src'] = author_images[i]['url']
                    auth['img_height'] = author_images[i]['img_height']
                    auth['img_width'] = author_images[i]['img_width']
                authors.append(auth)
            postJson["authors"] = authors
            postJson["date"] = DatePublished[0].text
            postJson["tags"] = tagArray
            postJson["post"] = postContent[0].text
            output["posts"].append(postJson)

        self.logger.info("Processing Completed!")
        return output["posts"]
