/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.path.internal;

import org.ratpackframework.path.PathBinding;
import org.ratpackframework.path.PathContext;
import org.ratpackframework.util.internal.Validations;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenPathBinding implements PathBinding {

  private final List<String> tokenNames;
  private final Pattern regex;
  private final boolean exact;

  public TokenPathBinding(String path, boolean exact) {
    this.exact = exact;
    Validations.noLeadingForwardSlash(path, "token path");

    List<String> names = new LinkedList<>();
    String regexString = Pattern.quote(path);

    Pattern placeholderPattern = Pattern.compile("(:\\w+)");
    Matcher matchResult = placeholderPattern.matcher(path);
    while (matchResult.find()) {
      String name = matchResult.group();
      regexString = regexString.replaceFirst(name, "\\\\E([^/?&#]+)\\\\Q");
      names.add(name.substring(1));
    }

    this.regex = Pattern.compile(regexString);
    this.tokenNames = Collections.unmodifiableList(names);
  }

  @Override
  public PathContext bind(String path, PathContext pathContext) {
    String regexString = regex.pattern();

    if (pathContext != null) {
      regexString = pathContext.join(regexString);
    }

    regexString = "(" + regexString + ")";
    if (!exact) {
      regexString = regexString.concat("(?:/.*)?");
    }

    Pattern bindPattern = Pattern.compile(regexString);

    Matcher matcher = bindPattern.matcher(path);
    if (matcher.matches()) {
      MatchResult matchResult = matcher.toMatchResult();
      String boundPath = matchResult.group(1);
      Map<String, String> params = new LinkedHashMap<>();
      int i = 2;
      for (String name : tokenNames) {
        params.put(name, matchResult.group(i++));
      }

      return new DefaultPathContext(path, boundPath, params, pathContext);
    } else {
      return null;
    }
  }
}