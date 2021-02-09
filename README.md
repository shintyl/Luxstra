# Luxstra

Research has shown that people are less willing to walk at night than they are at day. To many this is intuitively obvious, and a consequence of numerous factors; poor night vision, the belief that being victimized is more likely at night, or general discomfort with walking at night, being among them. Clearly, well-lit paths can benefit pedestrians, and even drivers and cyclists who would like to be able to clearly see their surroundings. The only problem is properly finding a way to get from point A to point B while staying in the light as much as possible.

Our application allows all those who need to move outside at night to automatically find the best illuminated path without sacrificing travel time. We are building a future where people can feel safe in their own communities, and are empowered to seek low-carbon alternatives – such as walking and biking – to traditional modes of transportation.

Our stack consists of React with the material-ui framework on the front end, SparkJava running on a Heroku server, PostgreSQL as our database, and Github for repository storage and continuous integration with Heroku. We use a variety of other npm and maven packages, including HikariCP, google-maps-react, and dotenv.

To compare and test routes against locations of street lights, we first needed to assimilate streetlight data from the city most of us were working from, Baltimore, into a locally-deployable database. This involved not only finding and extracting the locations of nearly 10,000 streetlights from online databases, but also remoulding the data into an appropriate format to be used by the backend. We used multiple preprocessing steps on the initial streetlight data by running through the .json file through 2 different Google Map APIs involving proprietary street information, and injecting that into the existing data. Finally, we wrote an intermediary Java program to expeditiously add this dataset to a database on AWS.

After deciding on an algorithm to highlight the salient light features, we needed a streamlined way to process data from the database through various Google Maps API calls, polyline encodings, and the final algorithm. To this end, we used the Java Stream API, with a functional programming paradigm to quickly transform, filter, and validate data.

Built for [UGAHacks 2021](https://devpost.com/software/luxstra) at University of Georgia
- Winner of First Place Overall
- Winner of Best use of Google Cloud
- Winner of State Farm Challenge
