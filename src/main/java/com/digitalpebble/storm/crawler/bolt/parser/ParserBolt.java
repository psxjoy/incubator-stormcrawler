package com.digitalpebble.storm.crawler.bolt.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

import com.digitalpebble.storm.crawler.StormConfiguration;

/**
 * Uses Tika to parse the output of a fetch and extract text + metadata
 ***/

@SuppressWarnings("serial")
public class ParserBolt extends BaseRichBolt {

	private Configuration config;

	private Tika tika;

	private OutputCollector collector;

	private static final org.slf4j.Logger LOG = LoggerFactory
			.getLogger(ParserBolt.class);

	public void prepare(Map conf, TopologyContext context,
			OutputCollector collector) {
		config = StormConfiguration.create();

		// instanciate Tika
		long start = System.currentTimeMillis();
		tika = new Tika();
		long end = System.currentTimeMillis();

		LOG.debug("Tika loaded in "+(end-start)+" msec");

		this.collector = collector;
	}

	public void execute(Tuple tuple) {
		// rely on mime-type provided by server or guess?

		byte[] content = tuple.getBinaryByField("content");
		String url = tuple.getStringByField("url");

		// TODO check status etc...

		long start = System.currentTimeMillis();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(content);
		Metadata md = new Metadata();

		String text = null;

		// parse
		try {
			text = tika.parseToString(bais, md);
		} catch (Exception e) {
			LOG.error("Exception while parsing with Tika", e);
			// TODO clean exit
		} finally {
			try {
				bais.close();
			} catch (IOException e) {
				LOG.error("Exception while closing stream", e);
			}
		}

		long end = System.currentTimeMillis();
		
		LOG.debug("Parsed "+url +"in "+(end-start)+" msec");
		
		// TODO process outlinks and metadata

		// generate output
		List<Object> fields = tuple.getValues();
		fields.add(text.trim());

		collector.emit(fields);
		collector.ack(tuple);

	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// output of this module is the list of fields to index
		// with at least the URL, text content

		declarer.declare(new Fields("url", "content", "text"));
	}

}