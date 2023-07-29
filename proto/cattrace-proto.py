import sys
import json
import perfetto_trace_pb2 as Perfetto

# https://android.googlesource.com/platform/external/perfetto/+/refs/heads/main/protos/
# https://android.googlesource.com/platform/external/perfetto/+/refs/heads/main/protos/perfetto/trace/perfetto_trace.proto
# protoc -I=. --python_out=. ./perfetto_trace.proto

# https://perfetto.dev/docs/reference/trace-packet-proto
# https://perfetto.dev/docs/reference/synthetic-track-event

TAG = "CatTrace"

EVENT_TYPE_SYNC = "clock_sync"
EVENT_TYPE_BEGIN = "B"
EVENT_TYPE_END = "E"
EVENT_TYPE_COMPLETE = "X"
EVENT_TYPE_INSTANT = "i"
EVENT_TYPE_METADATA = "M"

GLOBAL_FILE = "global"

TRACES = {}

def parse(text):
	try:
		return None, json.loads(text)
	except Exception as e:
		return e, None

def write(root, path):
	with open(f"{path}.perfetto_trace", "wb") as fd:
		fd.write(root.SerializeToString())

def read(path):
	r = Perfetto.Trace()
	with open(f"{path}.perfetto_trace", "rb") as fd:
		r.ParseFromString(fd.read())
	return r

def new_process(uuid, pid, name):
	event = Perfetto.TrackDescriptor()
	event.uuid = uuid
	process = Perfetto.ProcessDescriptor()
	process.pid = pid
	process.process_name = name
	event.process.CopyFrom(process)

	packet = Perfetto.TracePacket()
	packet.track_descriptor.CopyFrom(event)

	return packet

def new_thread(uuid, parent_uuid, pid, tid, name):
	event = Perfetto.TrackDescriptor()
	event.uuid = uuid
	event.parent_uuid = parent_uuid
	thread = Perfetto.ThreadDescriptor()
	thread.pid = pid
	thread.tid = tid
	thread.thread_name = name
	event.thread.CopyFrom(thread)

	packet = Perfetto.TracePacket()
	packet.track_descriptor.CopyFrom(event)

	return packet

def new_event(track_uuid, event_type, timestamp, name = None):
	packet = Perfetto.TracePacket()
	packet.timestamp = timestamp
	packet.trusted_packet_sequence_id = TRUSTED_PACKET_SEQUENCE_ID

	track_event = Perfetto.TrackEvent()
	track_event.type = event_type
	track_event.track_uuid = track_uuid
	if name:
		track_event.name = name

	packet.track_event.CopyFrom(track_event)
	return packet

TRUSTED_PACKET_SEQUENCE_ID = 3903809   # Generate *once*, use throughout. Can we just the same everytime we convert a trace?

# Replace with a more reliable method?
CURRENT_UUID = 0

def uuid():
	global CURRENT_UUID
	CURRENT_UUID = CURRENT_UUID + 1
	return CURRENT_UUID

def main():
	# Replicates the example in https://perfetto.dev/docs/reference/synthetic-track-event

	# This is is incomplete but shows how you would convert the events to a protobuf binary file.
	# There's also # https://androidx.tech/artifacts/benchmark/benchmark-common/1.1.0-beta06-source/androidx/benchmark/UserspaceTracing.kt.html 
	# which would allow you to write the file directly on the device.

	root = Perfetto.Trace()
	print(f"{root=}")

	process_uuid = 894893984
	pid = 1234
	new_process_packet = new_process(process_uuid, pid, "Name of Process")	
	thread_uuid = 49083589894
	new_thread_packet = new_thread(thread_uuid, process_uuid, pid, 5678, "Main Thread")
	
	root.packet.extend([
		new_process_packet,
	 	new_thread_packet,
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_BEGIN, 200, "My special parent"),
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_BEGIN, 250, "My special child"),
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_INSTANT, 285, ""),
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_END, 290, ""),
	 	new_event(thread_uuid, Perfetto.TrackEvent.Type.TYPE_SLICE_END, 300, ""),
	 	])

	write(root, "output")
	r = read("output")
	print(f"{r=}")

if __name__ == '__main__':
	main()
