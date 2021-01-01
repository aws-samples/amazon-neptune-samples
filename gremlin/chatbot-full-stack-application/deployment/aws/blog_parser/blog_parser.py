# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0
from requests import get
from requests.exceptions import RequestException
from contextlib import closing
from bs4 import BeautifulSoup
from PIL import Image
from io import BytesIO
import base64
import sys
import json
import argparse
import logging
import urllib


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
    
    def __resize_image(self, img):
        max_size=100
        # resize the image to a max dimension of max_size
        width=img.size[0]
        height=img.size[1]
        if width>max_size or height>max_size:
            ratio=1
            size=()
            if width>height: # horizontal image
                ratio = max_size/width
                size=(max_size, height*ratio)
            else: # vertical image
                ratio = max_size/height
                size=(width*ratio, max_size)

            img.thumbnail(size,Image.ANTIALIAS)
        return img

    def __url_to_datauri(self, url):
        urllib.request.urlretrieve(url, "sample.png")
        img = Image.open("sample.png")
        img=self.__resize_image(img)
        #converts image to datauri
        data = BytesIO()
        img.save(data, "PNG")
        data64 = base64.b64encode(data.getvalue())
        return {'img_src': url,
                'thumbnail': u'data:img/png;base64,'+data64.decode('utf-8'),
                'img_height': img.size[1],
                'img_width': img.size[0]}

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
                            author_images.append(self.__url_to_datauri(children[0]['src']))
                            


            tagArray = []
            authorArray = []
            for tag in Categories:
                tagArray.append(tag.text)
            for auth in Authors:
                authorArray.append(auth.text)
            postJson = self.__url_to_datauri(post["img_src"])
            postJson["url"] = post["url"]
            postJson["title"] = Title[0].text
            authors = []
            i = 0
            for i, a in enumerate(authorArray):
                auth = {'name': a}
                if i < len(author_images):
                    auth['img_src'] = author_images[i]['img_src']
                    auth['thumbnail'] = author_images[i]['thumbnail']
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
