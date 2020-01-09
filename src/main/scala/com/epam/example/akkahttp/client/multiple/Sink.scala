package com.epam.example.akkahttp.client.multiple

import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.typesafe.config.Config


trait Sink {
  //func init() {
  def init()

  // connect(h *HTTP) () error {
  def connect(httpConf: Config)

  //func (h *HTTP) createClient(ctx context.Context) (*http.Client, error) {
  def createClient(httpConf: Config, ctx: Object, client: Object)

  //func (h *HTTP) Write(metrics []telegraf.Metric) error {
  def write(httpConf: Config, event: AppEvent)

  // (h *HTTP) Close() error {
  def close(httpConf: Config)
}
