import threading
from io import StringIO

class StatisticsAccumulator():
    def __init__(self):
        self.input_tokens = 0
        self.output_tokens = 0
        self.cache_read_input_tokens = 0
        self.cache_write_input_tokens = 0
        self.stats_by_task = {}
        self._lock = threading.Lock()

    def accumulate(self, key, dict_statistics):
        try:
            with self._lock:
                self.input_tokens = self.input_tokens + dict_statistics['usage']["inputTokens"]
                self.output_tokens = self.output_tokens + dict_statistics['usage']["outputTokens"]
                if "cacheReadInputTokens" in dict_statistics['usage']:
                    self.cache_read_input_tokens = self.cache_read_input_tokens + dict_statistics['usage']["cacheReadInputTokens"]
                if "cacheWriteInputTokens" in dict_statistics['usage']:
                    self.cache_write_input_tokens = self.cache_write_input_tokens + dict_statistics['usage']["cacheWriteInputTokens"]
                self.stats_by_task[key] = dict_statistics
        except Exception as e:
            print(f"Exception {e}")
            print(f"Statistics object is:\n{dict_statistics}")
    

    def print_summary_statistics(self):
        with self._lock:
            return f"""
                Total Tokens Utilized:
                    Input Tokens: {self.input_tokens}
                    Output Tokens: {self.output_tokens}
                    Cache Read Input Tokens: {self.cache_read_input_tokens}
                    Cache Write Input Tokens: {self.cache_write_input_tokens}
                Total Estimated Cost: {(self.input_tokens / 1000.0 * 0.003) + (self.output_tokens / 1000.0 * 0.015) + (self.cache_read_input_tokens / 1000.0 * 0.0003) + (self.output_tokens / 1000.0 * 0.00375)}
            """

    def print_all_statistics(self):
        with self._lock:
            buffer = StringIO()
            for key in self.stats_by_task:
                buffer.write(f"{key}: \n{self.stats_by_task[key]}\n")
            buffer.seek(0)
            return buffer.read()