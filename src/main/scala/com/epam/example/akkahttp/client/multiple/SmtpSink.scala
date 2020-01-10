package com.epam.example.akkahttp.client.multiple

import java.util.Properties

import javax.mail.{Message, Session, Transport}
import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import javax.mail.internet.{InternetAddress, MimeMessage}


class SmtpSink extends Sink {

  val log: Logger = Logger(getClass.getName)

  var transport: Transport = _
  var session: Session = _
  var fromEmail: String = _
  var toEmails: java.util.List[String] = _

  override def init(conf: Config): Unit = {
    val smtpHost = conf.getString("smtp.host")
    val smtpPort = conf.getString("smtp.port")
    val username = conf.getString("username")
    val password = conf.getString("password")

    fromEmail = conf.getString("from")
    toEmails = conf.getStringList("to")

    val props = new Properties()
    props.put("mail.smtp.host", "smtp.gmail.com")
    props.put("mail.smtp.socketFactory.port", "465")
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.port", "465")
//    props.put("mail.smtp.port", "465")
//    props.put("mail.smtp.ssl.enable", "true")
    props.put("mail.debug", "true")
    val session = Session.getDefaultInstance(props, null)
    transport = session.getTransport("smtp")
    transport.connect(smtpHost, Integer.parseInt(smtpPort), username, password)
    log.info("Created SMTP Client")
  }

  override def write(httpConf: Config, event: AppEvent): Unit = {
    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(fromEmail), )
    message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmails.get(0)))
    message.setSubject("New Event #" + event.id)
    message.setContent(event.text, "text/html")
    transport.sendMessage(message, message.getAllRecipients)
  }

  override def close(httpConf: Config): Unit = {
    log.info("Terminate SMTP Client...")
    transport.close()
  }
}
