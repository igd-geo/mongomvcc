require "buildr/bnd"

repositories.remote << 'http://repo1.maven.org/maven2'
repositories.remote << Buildr::Bnd.remote_repository

repositories.release_to = 'https://oss.sonatype.org/service/local/staging/deploy/maven2'

MONGODB = 'org.mongodb:mongo-java-driver:jar:2.7.3'
TROVE = 'net.sf.trove4j:trove4j:jar:3.0.2'
GUAVA = 'com.google.guava:guava:jar:11.0.2'

define 'mongomvcc' do
  project.version = '0.3.0'
  project.group = 'de.fhg.igd'
  
  compile.with MONGODB, TROVE, GUAVA
  compile.options.lint = 'all'
  
  package(:bundle).tap do |bnd|
    bnd['Import-Package'] = "*"
    bnd['Export-Package'] = "de.fhg.igd.*;version=#{version}"
    bnd['Bundle-Vendor'] = 'Fraunhofer IGD'
    bnd['Include-Resource'] = _('LICENSE.txt')
  end
  package(:bundle).pom.from create_pom(package(:bundle), compile.dependencies)
  package :sources
  package :javadoc
  
  # sign artifacts before uploading
  packages.each { |p| sign_artifact(p) }
  sign_artifact(package(:bundle).pom)
end

def create_pom(pkg, deps)
 file(_(:target, "pom.xml")) do |file|
   Dir.mkdir(_(:target)) unless FileTest.exists?(_(:target))
   File.open(file.to_s, 'w') do |f|
     xml = Builder::XmlMarkup.new(:target => f, :indent => 2)
     xml.instruct!
     xml.project do
       xml.modelVersion "4.0.0"
       xml.groupId pkg.group
       xml.artifactId pkg.id
       xml.packaging 'jar'
       xml.version pkg.version
       xml.name pkg.id
       xml.description 'Implements the MVCC model on top of MongoDB'
       xml.url 'http://www.igd.fraunhofer.de/geo'
       xml.licenses do
         xml.license do
           xml.name 'GNU Lesser General Public License (LGPL) v3.0'
           xml.url 'http://www.gnu.org/licenses/lgpl-3.0.txt'
           xml.distribution 'repo'
         end
       end
       xml.scm do
         xml.connection 'scm:git:git://github.com/igd-geo/mongomvcc.git'
         xml.url 'scm:git:git://github.com/igd-geo/mongomvcc.git'
         xml.developerConnection 'scm:git:git://github.com/igd-geo/mongomvcc.git'
       end
       xml.developers do
         xml.developer do
           xml.id 'michel-kraemer'
           xml.name 'Michel Kraemer'
           xml.email 'michel@undercouch.de'
         end
       end
       xml.dependencies do
         deps.each do |artifact|
           xml.dependency do
             xml.groupId artifact.group
             xml.artifactId artifact.id
             xml.version artifact.version
             xml.scope 'compile'
           end
         end
       end
     end
   end
 end
end

def sign_artifact(p)
  artifact = Buildr.artifact(p.to_spec_hash.merge(:type => "#{p.type}.asc"))
  asc = file(p.to_s + '.asc') do
    sh %{gpg -ab "#{p.to_s}"}
  end
  artifact.from asc
  task(:upload).enhance [ artifact.upload_task ]
end
