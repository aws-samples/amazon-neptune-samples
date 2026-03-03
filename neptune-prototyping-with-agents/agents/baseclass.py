from strands import Agent
from strands.types.exceptions import EventLoopException
from botocore.exceptions import EventStreamError, ClientError
import logging
import time
logger = logging.getLogger(__name__)

class BaseAgent:
    def __init__(self, name):
        self._agent = None
        self._result = None
        self._name = name
    
    def get_agent(self):
        return self._agent
    
    def get_statistics(self):
        if (not hasattr(self, "_result") or self._result is None):
            return {}
        
        return {
          'cycles': self._result.metrics.cycle_count,
          'tools' : self._result.metrics.tool_metrics,
          'usage' : self._result.metrics.accumulated_usage,
          'accumulated_metrics' : self._result.metrics.accumulated_metrics
        }
    
    def get_metrics_summary(self):
        if (not hasattr(self, "_result") or self._result is None):
            return {}

        return self._result.metrics.get_summary()

    def execute_task(self, **kwargs):
        MAX_EVENT_LOOP_RETRIES = 10
        finished = False
        output = None
        retry_count = 0

        while (not finished and retry_count < MAX_EVENT_LOOP_RETRIES):
            try: 
                self._result = self._execute_agent(**kwargs)
                finished = True
            except EventLoopException as e:
                retry_count += 1
                print(f"EventLoopException caught, retrying {self._name} task...retry: {retry_count} of {MAX_EVENT_LOOP_RETRIES}")
                if retry_count >= MAX_EVENT_LOOP_RETRIES:
                    print(f"Max retries reached for {self._name} task. Exiting.")
                    raise e
            except EventStreamError as ese:
                retry_count += 1
                print(f"EventStreamError caught, retrying {self._name} task...retry: {retry_count} of {MAX_EVENT_LOOP_RETRIES}")
                if retry_count >= MAX_EVENT_LOOP_RETRIES:
                    print(f"Max retries reached for {self._name} task. Exiting.")
                    raise ese
            except ClientError as ce:
                retry_count += 1
                if ce.response['Error']['Code'] == 'ServiceUnavailableException':
                    print(f"ServiceUnavailableException caught (via ClientError), retrying {self._name} task...retry: {retry_count} of {MAX_EVENT_LOOP_RETRIES}. Sleeping for 30 seconds before retrying...")
                    # nosemgraph: python.lang.best-practice.arbitrary-sleep
                    time.sleep(30)
                else:
                    print(f"Unknown ClientError caught, retrying {self._name} task...retry: {retry_count} of {MAX_EVENT_LOOP_RETRIES}. Exception: {ce}")
                if retry_count >= MAX_EVENT_LOOP_RETRIES:
                    print(f"Max retries reached for {self._name} task. Exiting.")
                    raise ce
            except Exception as e:
                print(f"Error is in {self._name} task: {e}")
                print(f"Type of exception: {type(e)}")
                print(f"Exiting thread task due to error for {self._name}.")
                raise e
        print(f"Finished {self._name} task after {retry_count} retries.")
        return str(self._result)

    def _execute_agent(self, **kwargs):
        """
        The code to actually execute the agent.
        This method must be implemented by subclasses.
        """
        raise NotImplementedError("Subclasses must implement the 'execute_agent' method.")