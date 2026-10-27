#!/bin/sh

# Copyright (c) 2020-2026 dzikoysk
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -eu

cp "$(dirname "$0")/blocked.certs" /tmp/blocked.certs
cat "$JAVA_HOME/lib/security/blocked.certs" >> /tmp/blocked.certs
sort -u /tmp/blocked.certs -o "$JAVA_HOME/lib/security/blocked.certs"
rm /tmp/blocked.certs

keytool -J-Duser.language=en -J-Duser.country=US -list -v -cacerts -storepass changeit > /tmp/java-truststore.txt
awk '
    /^Alias name: / { alias = substr($0, 13) }
    /^Owner: / && /CN=(emSign Root CA - G[12]|emSign ECC Root CA - G3|AAA Certificate Services)(,|$)/ { print alias }
' /tmp/java-truststore.txt | while IFS= read -r alias; do
    keytool -delete -cacerts -storepass changeit -alias "$alias"
done
rm /tmp/java-truststore.txt
