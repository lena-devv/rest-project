package com.epam.example.akkahttp.client.multiple

import java.util.Properties

import javax.mail.{Address, Message, Session, Transport}
import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import javax.mail.internet.{InternetAddress, MimeMessage}

import scala.collection.JavaConversions._


class SmtpSink extends Sink {

  val log: Logger = Logger(getClass.getName)
  val SUBJECT_TEMPLATE = "New Event #"

  var transport: Transport = _
  var session: Session = _

  var fromAddress: Address = _
  var toAddresses: Array[Address] = _

  override def init(conf: Config): Unit = {
    val smtpHost = conf.getString("smtp.host")
    val smtpPort = conf.getString("smtp.port")
    val username = conf.getString("username")
    val password = conf.getString("password")

    val fromEmail = conf.getString("from")
    fromAddress = new InternetAddress(fromEmail, "App Notifications")
    toAddresses = conf.getStringList("to")
      .map(email => new InternetAddress(email))
      .toArray[Address]

    val props = new Properties()
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.debug", "true")
    val session = Session.getDefaultInstance(props, null)
    transport = session.getTransport("smtp")
    transport.connect(smtpHost, Integer.parseInt(smtpPort), username, password)

    log.info("Created SMTP Client")
  }

  override def write(httpConf: Config, event: AppEvent): Unit = {
    val message = new MimeMessage(session)
    message.setFrom(fromAddress)
    message.addRecipients(Message.RecipientType.TO, toAddresses)
    message.setSubject(SUBJECT_TEMPLATE + event.id)
    message.setContent(event.toString, "text/html")
    transport.sendMessage(message, message.getAllRecipients)
  }

  override def close(httpConf: Config): Unit = {
    log.info("Terminate SMTP Client...")
    transport.close()
  }
}
