#   (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
#   All rights reserved. This program and the accompanying materials
#   are made available under the terms of the Apache License v2.0 which accompany this distribution.
#
#   The Apache License is available at
#   http://www.apache.org/licenses/LICENSE-2.0

namespace: user.decisions

decision:
  name: decision_1
  inputs:
    - x
    - y
  outputs:
    - sum: ${str(int(x)+int(y))}
  results:
    - EQUAL: ${int(x) == int(y)}
    - LESS_THAN: ${int(x) < int(y)}
    - GREATER_THAN
