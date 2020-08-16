/*
 * Copyright (c) 2020 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* eslint-disable */
(function scrollSpy() {
  var OFFSET = 10;
  var timer;
  var headingsCache;

  var findHeadings = function findHeadings() {
    return headingsCache || document.querySelectorAll('.toc-headings > li > a');
  };

  var onScroll = function onScroll() {
    if (timer) {
      // throttle
      return;
    }

    timer = setTimeout(function() {
      timer = null;
      var activeNavFound = false;
      var headings = findHeadings(); // toc nav anchors

      /**
       * On every call, try to find header right after  <-- next header
       * the one whose content is on the current screen <-- highlight this
       */

      for (var i = 0; i < headings.length; i++) {
        // headings[i] is current element
        // if an element is already active, then current element is not active
        // if no element is already active, then current element is active
        var currNavActive = !activeNavFound;
        /**
         * Enter the following check up only when an active nav header is not yet found
         * Then, check the bounding rectangle of the next header
         * The headers that are scrolled passed will have negative bounding rect top
         * So the first one with positive bounding rect top will be the nearest next header
         */

        if (currNavActive && i < headings.length - 1) {
          var heading = headings[i + 1];
          var next = decodeURIComponent(heading.href.split('#')[1]);
          var nextHeader = document.getElementById(next);

          if (nextHeader) {
            var top = nextHeader.getBoundingClientRect().top;
            currNavActive = top > OFFSET;
          } else {
            console.error('Can not find header element', {
              id: next,
              heading: heading,
            });
          }
        }
        /**
         * Stop searching once a first such header is found,
         * this makes sure the highlighted header is the most current one
         */

        if (currNavActive) {
          activeNavFound = true;
          headings[i].classList.add('active');
        } else {
          headings[i].classList.remove('active');
        }
      }
    }, 100);
  };

  document.addEventListener('scroll', onScroll);
  document.addEventListener('resize', onScroll);
  document.addEventListener('DOMContentLoaded', function() {
    // Cache the headings once the page has fully loaded.
    headingsCache = findHeadings();
    onScroll();
  });
})();
