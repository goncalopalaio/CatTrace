import sys
import json


TAG = "CatTrace"

EVENT_TYPE_SYNC = "clock_sync"
EVENT_TYPE_BEGIN = "B"
EVENT_TYPE_END = "E"
EVENT_TYPE_COMPLETE = "X"
EVENT_TYPE_INSTANT = "i"
EVENT_TYPE_METADATA = "M"

GLOBAL_PID = -1

TRACES = {}

SEEN_PIDS = {}

def main():
	print("main")
	while True:
		line = sys.stdin.readline()
		line = line.strip()

		if not line:
			break
		
		print(f"{line=}")
		
		start = line.find(TAG)
		if start < 0:
			print(f"Failed to find tag | {line=}")
			continue

		start = len(TAG) + start + 1

		parts = line[start:].split("|")
		print(f"{parts=}")

		event_type = parts[0]
		pid = int(parts[1])

		if pid not in SEEN_PIDS:
			print("\n\n")
			print(f"New pid | {pid=}")
			print("\n\n")
			SEEN_PIDS[pid] = pid

		event = None
		if event_type == EVENT_TYPE_SYNC:
			time = int(parts[2])
			event = create_sync(time)

		elif event_type == EVENT_TYPE_BEGIN:
			thread_id = int(parts[2])
			thread_name = parts[3]
			time = int(parts[4])
			name = parts[5]
			event = create_begin(pid, thread_id, thread_name, time, name)

		elif event_type == EVENT_TYPE_END:
			thread_id = int(parts[2])
			thread_name = parts[3]
			time = int(parts[4])
			name = parts[5]
			event = create_end(pid, thread_id, thread_name, time, name)

		elif event_type == EVENT_TYPE_COMPLETE:
			thread_id = int(parts[2])
			thread_name = parts[3]
			time = int(parts[4])
			duration = int(parts[5])
			name = parts[6]
			event = create_complete(pid, thread_id, time, duration, name)

		elif event_type == EVENT_TYPE_INSTANT:
			thread_id = int(parts[2])
			thread_name = parts[3]
			instant_type = parts[4]
			time = int(parts[5])
			name = parts[6]
			event = create_instant(pid, thread_id, time, instant_type, name)
		elif event_type == EVENT_TYPE_METADATA:
			thread_id = int(parts[2])
			metadata_type = parts[3]
			metadata_value = parts[4]
			event = create_metadata(pid, thread_id, metadata_type, metadata_value)

		else:
			print(f"Ignoring event | {line=}")
			continue

		if pid not in TRACES:
			TRACES[pid] = []

		if GLOBAL_PID not in TRACES:
			TRACES[GLOBAL_PID] = []

		TRACES[pid].append(event)
		TRACES[GLOBAL_PID].append(event)

		print(f"{event=} | {len(TRACES[pid])} {len(TRACES[GLOBAL_PID])}")

		dump_events(f"pid-{pid}", TRACES[pid])
		dump_events("global", TRACES[GLOBAL_PID])


def create_begin(pid, thread_id, thread_name, time, name):
	return {"name": name, "ph": "B", "ts": time, "pid": pid, "tid": thread_id}

def create_end(pid, thread_id, thread_name, time, name):
	return {"name": name, "ph": "E", "ts": time, "pid": pid, "tid": thread_id}

def create_sync(time):
	return { "name": "clock_sync", "ph": "c", "ts": time, "args": { "sync_id": "guid1" } }

def create_complete(pid, thread_id, time, duration, name):
	return {"name": name, "ph": "X", "ts": time, "dur": duration, "pid": pid, "tid": thread_id }

def create_instant(pid, thread_id, time, instant_type, name):
	return {"name": name, "ph": "i", "ts": time, "pid": pid, "tid": thread_id, "s": instant_type}

def create_metadata(pid, thread_id, metadata_type, metadata_value):
	return {"name": metadata_type, "ph": "M", "pid": pid, "tid": thread_id, "args": { "name" : metadata_value } }

def create_trace(events):
	# return { "traceEvents": events, "displayTimeUnit": "us"} # Perfetto is ignoring displayTimeUnit according to their error pane.
	return { "traceEvents": events }

def dump_events(file_name, events):
	trace = create_trace(events)

	with open(f"{file_name}.json", 'w') as fp:
		json.dump(trace, fp)

if __name__ == '__main__':
	main()
