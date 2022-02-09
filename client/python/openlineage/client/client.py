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

import logging
from typing import Optional

import attr
from requests import Session
from requests.adapters import HTTPAdapter

from openlineage.client.run import RunEvent
from openlineage.client.transport import Transport, get_default_factory
from openlineage.client.transport.http import HttpTransport, HttpConfig


@attr.s
class OpenLineageClientOptions:
    timeout: float = attr.ib(default=5.0)
    verify: bool = attr.ib(default=True)
    api_key: str = attr.ib(default=None)
    adapter: HTTPAdapter = attr.ib(default=None)


log = logging.getLogger(__name__)


class OpenLineageClient:
    def __init__(
        self,
        url: Optional[str] = None,
        options: Optional[OpenLineageClientOptions] = None,
        session: Optional[Session] = None,
        transport: Optional[Transport] = None,
    ):
        if url:
            # Backwards compatibility: if URL is set, use old path to initialize
            # HTTP transport.
            if not options:
                options = OpenLineageClientOptions()
            if not session:
                session = Session()
            self._initialize_url(url, options, session)
        elif transport:
            self.transport = transport
        else:
            self.transport = get_default_factory().create()

    def _initialize_url(
        self,
        url: str,
        options: OpenLineageClientOptions,
        session: Session
    ):
        self.transport = HttpTransport(HttpConfig(
            url=url,
            timeout=options.timeout,
            verify=options.verify,
            api_key=options.api_key,
            session=session,
            adapter=options.adapter
        ))

    def emit(self, event: RunEvent):
        if not isinstance(event, RunEvent):
            raise ValueError("`emit` only accepts RunEvent class")
        self.transport.emit(event)

    @classmethod
    def from_environment(cls):
        return cls(transport=get_default_factory().create())
