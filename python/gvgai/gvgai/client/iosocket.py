import logging
import socket
import sys
import traceback
import time
from struct import pack_into, unpack_from

class IOSocket:

    def __init__(self, client_only=False):
        self.HEADER_SIZE = 13
        self.BUFFER_SIZE = 8192*10
        self.hostname, self.port = self.getOpenAddress()
        self.connected = False
        self.socket = None
        self._last_message_id = 0

        # Default port if this is in client only mode
        if client_only:
            self.port = 8083

        self._logger = logging.getLogger("IOSocket")

    def initBuffers(self):
        self._logger.debug(f'Connecting to host {self.hostname} at port {self.port}')
        while not self.connected:
            try:
                self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
                if not 'darwin' in sys.platform:
                    self.socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_QUICKACK, 1)
                else:
                    self._logger.warn('Attempting to run without TCP_QUICKACK. Consider running instead on unix!')
                self.socket.connect((self.hostname, self.port))
                self.connected = True
                self._logger.debug("Client connected to server [OK]")
            except Exception as e:
                self._logger.error("Could not connect, trying again")
                time.sleep(1)

    def writeToServer(self, agent_phase, data=None):

        self._logger.debug(f'Writing to server phase {agent_phase}')

        payload_size = len(data) if data is not None else 0
        buffer_size = 13 + payload_size

        agent_phase = bytes([agent_phase.value])

        buffer = bytearray(buffer_size)

        # Long in java is 8 bytes
        pack_into('>q', buffer, 0, self._last_message_id)
        pack_into('c', buffer, 8, agent_phase)

        if data is not None:
            pack_into('>i', buffer, 9, payload_size)
            pack_into('%ds' % payload_size, buffer, 13, data)
        else:
            pack_into('>i', buffer, 9, 0)

        try:
            self.socket.send(buffer)
        except Exception as e:
            logging.exception(e)
            traceback.print_exc()
            sys.exit()

    def shutDown(self):
        self.socket.shutdown(0)

    def getOpenAddress(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(("localhost",0))
        address = s.getsockname()
        s.close()
        return address

    def _read_until(self, length):
        buffer = bytearray()
        while len(buffer) < length:
            recv_size = min(length-len(buffer), self.BUFFER_SIZE)
            buffer += bytearray(self.socket.recv(recv_size))
        return buffer

    
    def readFromServer(self):
        # Firstly read the header bytes
        header_buffer = self._read_until(self.HEADER_SIZE)
        self._last_message_id, game_phase, message_size = unpack_from('>qci', header_buffer, 0)
        data = None
        if message_size > 0:
            data = self._read_until(message_size)
        return game_phase, data

