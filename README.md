# Functional Programming in Scala Capstone project

This repository contains code developed during the Scala Functional Programming capstone project https://www.coursera.org/learn/scala-capstone/home/welcome

I chose to use Spark to scale the data processing as it is a good fit for massively parallel tile generation.  As I didn't have access to a Hadoop cluster, I ran it in stand-alone mode on a few nodes of my work cluster.  The resulting processing time was around 20-30mins.

If you look back through the commits you will get a good idea of how I targetted each milestone in turn before refactoring into a more coherent overall structure.

The resulting web application is available on the web at:
- https://s3-eu-west-1.amazonaws.com/scala-capstone/index.html
