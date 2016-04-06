package com.sebastianvoss.template.api.domain

case class User(id: Option[String], name: String)

class EntityNotFoundException(message: String) extends Exception(message)