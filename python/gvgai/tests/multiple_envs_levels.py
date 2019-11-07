import logging
import time
import numpy as np

from gvgai.gym import GVGAI_Env
from gvgai.utils.level_data_generator import SokobanGenerator

if __name__ == '__main__':

    # Turn debug logging on
    logging.basicConfig(level=logging.INFO)

    logger = logging.getLogger('Test Agent')

    level_generator = SokobanGenerator()


    def generate_level():
        config = {
            'prob_hole': 0.05,
            'prob_box': 0.05,
            'prob_wall': 0.3,
            'width': np.random.randint(4, 10),
            'height': np.random.randint(4, 10)
        }

        return level_generator.generate(1, config).__next__()


    start = time.time()
    frames = 0

    env1 = GVGAI_Env('sokoban-custom', tile_observations=True, level_data=generate_level())
    env2 = GVGAI_Env('sokoban-custom', tile_observations=True, level_data=generate_level())
    env3 = GVGAI_Env('sokoban-custom', tile_observations=True, level_data=generate_level())

    for i in range(100):

        env1.reset(level_data=generate_level())
        env2.reset(level_data=generate_level())
        env3.reset(level_data=generate_level())

        for t in range(1000):
            # choose action based on trained policy
            # do action and get new state and its reward
            action_id = np.random.randint(5)
            step1 = env1.step(action_id)
            step2 = env2.step(action_id)
            step3 = env3.step(action_id)

            #env1.render()
            #env2.render()
            #env3.render()

            frames += 1

            if t % 100 == 0:
                end = time.time()
                total_time = end - start
                fps = (frames / total_time)
                logger.info(f'frames per second: {fps * 3}')

            # break loop when terminal state is reached
            if step1[2]:
                env1.reset()

            if step2[2]:
                env2.reset()

            if step3[2]:
                env3.reset()