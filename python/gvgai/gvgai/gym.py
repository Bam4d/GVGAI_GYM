#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Simulate VGDL Games
"""
import gym
import numpy as np
from gym import spaces
import re
from gvgai import GVGAIClient


class GVGAI_Env(gym.Env):
    """
    Define a VGDL environment.
    The environment defines which actions can be taken at which point and
    when the agent receives which reward.
    """

    def __init__(self, environment_id=None,
                 level_data=None,
                 max_steps=-1,
                 pixel_observations=True,
                 tile_observations=False,
                 include_semantic_data=False,
                 client_only=False
                 ):

        self.__version__ = "0.0.2"
        metadata = {'render.modes': ['human', 'rgb_array']}

        # Get or create the client and set the environment
        self.GVGAI = GVGAIClient(client_only=client_only)

        self.GVGAI.reset(environment_id,
                         level_data,
                         max_steps=max_steps,
                         pixel_observations=pixel_observations,
                         include_semantic_data=include_semantic_data,
                         tile_observations=tile_observations
                         )

        self.viewer = None

        self._reset_env_params()

        self.environment_id = environment_id
        self.level_data = level_data
        self._pixel_observations = pixel_observations
        self._tile_observations = tile_observations
        self._include_semantic_data = include_semantic_data
        self._max_steps = max_steps

    def step(self, action):
        """
        The agent takes a step in the environment.
        Parameters
        ----------
        action : int
        Returns
        -------
        ob, reward, episode_over, info : tuple
            state (image) :
                An image of the current frame of the game
            reward (float) :
                Total reward (Philip: Should it be incremental reward? Check Atari)
            isOver (bool) :
                whether it's time to reset the environment again.
            info (dict):
                info that can be added for debugging
                info["winner"] == PLAYER_LOSES, PLAYER_WINS, NO_WINNER
        """
        self._observations, reward, isOver, info = self.GVGAI.step(action)

        return self._observations, reward, isOver, info

    def _reset_env_params(self):

        self.actions = self.GVGAI.actions
        self.action_space = spaces.Discrete(len(self.actions))

        self.world_dimensions = self.GVGAI.world_dimensions

        height, width = self.world_dimensions
        observation_space = np.array([width, height, 3])
        self.observation_space = spaces.Box(low=0, high=255, shape=observation_space, dtype=np.int32)

    def reset(self, id=None, environment_id=None, level_data=None):
        """
        Reset the state of the environment and returns an initial observation.
        Returns
        -------
        observation (object): the initial observation of the space.
        """

        if id is not None:
            environment_id = id
        self._set_environment(environment_id, level_data)

        self._observations = self.GVGAI.reset(self.environment_id, self.level_data,
                                               max_steps=self._max_steps,
                                               include_semantic_data=self._include_semantic_data,
                                               pixel_observations=self._pixel_observations,
                                               tile_observations=self._tile_observations
                                               )
        self._reset_env_params()

        return self._observations

    def render(self, mode='human'):
        if self._observations is None:
            return

        if mode == 'rgb_array':
            return self._observations
        elif mode == 'human' and self._pixel_observations:
            image = self._observations if not self._tile_observations else self._observations[0]
            if self.viewer is None:
                self.viewer = SimpleImageViewer(maxwidth=500)
            self.viewer.imshow(image)
            return self.viewer.isopen

    def close(self):
        if hasattr(self, 'viewer') and self.viewer is not None:
            self.viewer.close()
            self.viewer = None

    def _set_environment(self, environment_id, level_data):

        if environment_id is None and level_data is None:
            return

        self.close()

        if environment_id is not None:
            if environment_id.startswith('gvgai'):
                expression = 'gvgai-(?P<env>.+)-v(?P<version>.+)'
                env_name_parts = re.search(expression, environment_id)
                self.environment_id = env_name_parts.group('env')
            else:
                self.environment_id = environment_id

        if level_data is not None:
            self.level_data = level_data

    def get_action_meanings(self):
        return sorted(self.actions, key=lambda k: k.value)

    def get_keys_to_action(self):
        return {
            (ord('w'),): 1,
            (ord('a'),): 2,
            (ord('s'),): 3,
            (ord('d'),): 4,
            (ord(' '),): 5
        }

    def stop(self):
        self.close()
        if hasattr(self, 'GVGAI'):
            self.GVGAI.stop()

    def __del__(self):
        self.stop()

class SimpleImageViewer(object):

    def __init__(self, display=None, maxwidth=500):
        self.window = None
        self.isopen = False
        self.display = display
        self.maxwidth = maxwidth

        self._pyglet = __import__('pyglet')
        self._gl = self._pyglet.gl

    def imshow(self, arr):
        if self.window is None:
            height, width, _channels = arr.shape
            # if width > self.maxwidth:
            scale = self.maxwidth / width
            width = int(scale * width)
            height = int(scale * height)
            self.window = self._pyglet.window.Window(width=width, height=height,
                                                     display=self.display, vsync=False, resizable=True)
            self.width = width
            self.height = height
            self.isopen = True

            @self.window.event
            def on_resize(width, height):
                self.width = width
                self.height = height

            @self.window.event
            def on_close():
                self.isopen = False

        assert len(arr.shape) == 3, "You passed in an image with the wrong number shape"
        image = self._pyglet.image.ImageData(arr.shape[1], arr.shape[0],
                                             'RGB', arr.tobytes(), pitch=arr.shape[1] * -3)
        texture = image.get_texture()
        texture.width = self.width
        texture.height = self.height
        self.window.clear()
        self._gl.glTexParameteri(self._gl.GL_TEXTURE_2D, self._gl.GL_TEXTURE_MAG_FILTER, self._gl.GL_NEAREST)
        self._gl.glTexParameteri(self._gl.GL_TEXTURE_2D, self._gl.GL_TEXTURE_MIN_FILTER, self._gl.GL_NEAREST)
        self.window.switch_to()
        self.window.dispatch_events()
        texture.blit(0, 0)  # draw
        self.window.flip()

    def close(self):
        self.window.close()
        self.isopen = False

    def __del__(self):
        self.close()
