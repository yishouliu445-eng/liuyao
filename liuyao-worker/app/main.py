import argparse
import logging

from app.config.settings import load_settings
from app.task_runner.worker import Worker


def main() -> None:
    parser = argparse.ArgumentParser(description="liuyao worker")
    parser.add_argument("--once", action="store_true", help="run a single poll/claim cycle")
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    settings = load_settings()
    worker = Worker(settings)
    if args.once:
        worker.run_once()
        return
    worker.run_forever()


if __name__ == "__main__":
    main()
