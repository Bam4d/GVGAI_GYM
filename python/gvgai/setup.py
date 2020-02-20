from setuptools import setup, find_packages
import os
import logging
import sys
import subprocess

from gvgai.client.utils.logpipe import LogPipe

with open("../../README.md", "r") as fh:
    long_description = fh.read()

with open("../../LICENSE", "r") as fh:
    license = fh.read()

from setuptools.command.install import install
from setuptools.command.develop import develop


def gradle_install():
    logging.basicConfig(level=logging.DEBUG)
    logger = logging.getLogger("gradle installer")
    log_level = logging.DEBUG
    logpipe = LogPipe("JAVA", level=log_level)

    root_path = os.path.realpath(os.path.dirname(os.path.realpath(__file__)) + "/../../")

    logger.debug(f'{root_path}')
    logger.info(f'Building GVGAI Environment')

    cmd = [f'{root_path}/gradlew', 'clean', 'install']
    try:
        # Pump the logging output to a logger so we can see it
        subprocess.call(cmd, stdout=logpipe, stderr=logpipe, cwd=root_path)
    except subprocess.CalledProcessError as e:
        logger.error(f'exit code: {e.returncode}')
        logger.error(f'stderr: {e.stderr.decode(sys.getfilesystemencoding())}')

    logger.info(f'GVGAI Environment Installed')
    logpipe.close()


class GradleDevelopCommand(develop):

    def run(self):
        develop.run(self)
        gradle_install()


class GradleInstallCommand(install):

    def run(self):
        install.run(self)
        gradle_install()


setup(
    name='gvgai-gym',
    version="0.2.1",
    author_email="chrisbam4d@gmail.com",
    description="GVGAI Gym Python",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/bam4d/GVGAI_GYM",
    packages=find_packages(),
    license=license,
    install_requires=[
        "gym==0.15.6",
        "flatbuffers==1.11",
        "numpy==1.18.1"
    ],
    cmdclass={
        'install': GradleInstallCommand,
        'develop': GradleDevelopCommand,
    }
)
