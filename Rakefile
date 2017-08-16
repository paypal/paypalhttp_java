require 'rake'

spec = Gem::Specification.find_by_name 'releasinator'
load "#{spec.gem_dir}/lib/tasks/releasinator.rake"

task :default => :test

task :build do
  sh "./gradlew clean build"
end

task :test do
  sh "./gradlew clean test"
end

task :release_braintreehttp do
  sh "./gradlew :braintreehttp:uploadArchives  :braintreehttp-testutils:uploadArchives"
  sh "./gradlew :braintreehttp:closeRepository"

  puts "Sleeping for one minute to allow BraintreeHttp modules to close"
  sleep 60
  sh "./gradlew :braintreehttp:promoteRepository :braintreehttp-testutils:promoteRepository"

  puts "Sleeping for ten minutes to allow BraintreeHttp modules to be promoted"
  sleep 600
  puts "Braintreehttp has been released"
end


