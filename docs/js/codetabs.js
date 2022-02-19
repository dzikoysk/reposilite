/*
 * Copyright (c) 2022 dzikoysk
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
// Turn off ESLint for this file because it's sent down to users as-is.

/* eslint-disable */
window.addEventListener('load', function () {
  // add event listener for all tab
  document.querySelectorAll('.nav-link').forEach(function (el) {
    el.addEventListener('click', function (e) {
      var groupId = e.target.getAttribute('data-group');
      document
        .querySelectorAll('.nav-link[data-group='.concat(groupId, ']'))
        .forEach(function (el) {
          el.classList.remove('active');
        });
      document
        .querySelectorAll('.tab-pane[data-group='.concat(groupId, ']'))
        .forEach(function (el) {
          el.classList.remove('active');
        });
      e.target.classList.add('active');
      document
        .querySelector('#'.concat(e.target.getAttribute('data-tab')))
        .classList.add('active');
    });
  });
});
